package dev.brahmkshatriya.echo.playback

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.CallSuper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.CoroutineUtils.await
import dev.brahmkshatriya.echo.utils.CoroutineUtils.future
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.util.WeakHashMap

@UnstableApi
abstract class AndroidAutoCallback(
    open val context: Context,
    open val scope: CoroutineScope,
    open val extensionList: StateFlow<List<MusicExtension>>,
    open val downloadFlow: StateFlow<List<Downloader.Info>>
) : MediaLibrarySession.Callback {

    @CallSuper
    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return Futures.immediateFuture(
            LibraryResult.ofItem(browsableItem(ROOT, "", browsable = false), null)
        )
    }

    @OptIn(UnstableApi::class)
    @CallSuper
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
        val extensions = extensionList.value
        if (parentId == ROOT) return@future LibraryResult.ofItemList(
            extensions.map { it.toMediaItem(context) },
            null
        )
        val extId = parentId.substringAfter("$ROOT/").substringBefore("/")
        val extension = extensions.first { it.id == extId }
        val searchQuery = params?.extras?.getString("search_query") ?: ""
        val type = parentId.substringAfter("$extId/").substringBefore("/")
        when (type) {
            ALBUM -> extension.get<AlbumClient> {
                val id = parentId.substringAfter("$ALBUM/").substringBefore("/")
                val unloaded = (itemMap[id] as? EchoMediaItem.Lists.AlbumItem)?.album!!
                getTracks(context, id, page) {
                    val album = loadAlbum(unloaded)
                    album.toMediaItem() to loadTracks(album)
                }
            }

            PLAYLIST -> extension.get<PlaylistClient> {
                val id = parentId.substringAfter("$PLAYLIST/").substringBefore("/")
                val unloaded = (itemMap[id] as? EchoMediaItem.Lists.PlaylistItem)?.playlist!!
                getTracks(context, id, page) {
                    val playlist = loadPlaylist(unloaded)
                    playlist.toMediaItem() to loadTracks(playlist)
                }
            }

            RADIO -> extension.get<RadioClient> {
                val id = parentId.substringAfter("$RADIO/").substringBefore("/")
                val radio = (itemMap[id]!! as EchoMediaItem.Lists.RadioItem).radio
                getTracks(context, id, page) {
                    radio.toMediaItem() to loadTracks(radio)
                }
            }

            ARTIST -> extension.get<ArtistClient> {
                val id = parentId.substringAfter("$ARTIST/").substringBefore("/")
                val artist = loadArtist(Artist(id, ""))
                getShelves(artist).toMediaItems(context, extId, page)
            }

            USER -> extension.get<UserClient> {
                val id = parentId.substringAfter("$USER/").substringBefore("/")
                val unloaded = (itemMap[id] as? EchoMediaItem.Profile.UserItem)?.user!!
                val user = loadUser(unloaded)
                getShelves(user).toMediaItems(context, extId, page)
            }

            LIST -> extension.get<ExtensionClient> {
                val id = parentId.substringAfter("$LIST/").substringBefore("/")
                getListsItems(context, id, extId)
            }

            SHELF -> extension.get<ExtensionClient> {
                val id = parentId.substringAfter("$SHELF/").substringBefore("/")
                getShelfItems(context, id, extId, page)
            }

            HOME -> extension.getFeed<HomeFeedClient>(
                context, parentId, HOME, page, { getHomeTabs() }, { getHomeFeed(it).pagedData }
            )

            LIBRARY -> extension.getFeed<LibraryFeedClient>(
                context, parentId, LIBRARY, page, { getLibraryTabs() }, { getLibraryFeed(it).pagedData }
            )

            SEARCH -> extension.getFeed<SearchFeedClient>(
                context, parentId, SEARCH, page,
                { searchTabs(searchQuery) }, { searchFeed(searchQuery, it).pagedData }
            )

            else -> LibraryResult.ofItemList(
                listOfNotNull(
                    if (extension.isClient<HomeFeedClient>())
                        browsableItem("$ROOT/$extId/$HOME", context.getString(R.string.home))
                    else null,
                    if (extension.isClient<SearchFeedClient>())
                        browsableItem("$ROOT/$extId/$SEARCH", context.getString(R.string.search))
                    else null,
                    if (extension.isClient<LibraryFeedClient>())
                        browsableItem("$ROOT/$extId/$LIBRARY", context.getString(R.string.library))
                    else null,
                ),
                null
            )
        }
    }

    @OptIn(UnstableApi::class)
    @CallSuper
    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return scope.future {
            val extensions = extensionList.value
            LibraryResult.ofItemList(
                extensions.map { ext ->
                    browsableItem("$ROOT/${ext.id}/$SEARCH", ext.name, query)
                },
                MediaLibraryService.LibraryParams.Builder()
                    .setExtras(bundleOf("search_query" to query))
                    .build()
            )
        }
    }

    @CallSuper
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) = scope.future {
        val new = mediaItems.mapNotNull {
            if (it.mediaId.startsWith("auto/")) {
                val id = it.mediaId.substringAfter("auto/")
                val (track, extId, con) =
                    context.getFromCache<Triple<Track, String, EchoMediaItem?>>(id, "auto")
                        ?: return@mapNotNull null
                MediaItemUtils.build(context, downloadFlow.value, track, extId, con)
            } else it
        }
        val future = super.onSetMediaItems(
            mediaSession, controller, new, startIndex, startPositionMs
        )
        future.await(context)
    }

    companion object {
        private const val ROOT = "root"
        private const val LIBRARY = "library"
        private const val HOME = "home"
        private const val SEARCH = "search"
        private const val SHELF = "shelf"
        private const val LIST = "list"

        private const val ARTIST = "artist"
        private const val USER = "user"
        private const val ALBUM = "album"
        private const val PLAYLIST = "playlist"
        private const val RADIO = "radio"

        private fun Resources.getUri(int: Int): Uri {
            val scheme = ContentResolver.SCHEME_ANDROID_RESOURCE
            val pkg = getResourcePackageName(int)
            val type = getResourceTypeName(int)
            val name = getResourceEntryName(int)
            val uri = "$scheme://$pkg/$type/$name"
            return uri.toUri()
        }

        private fun ImageHolder.toUri(context: Context) = when (this) {
            is ImageHolder.UriImageHolder -> uri.toUri()
            is ImageHolder.UrlRequestImageHolder -> request.url.toUri()
            is ImageHolder.ResourceImageHolder -> context.resources.getUri(resId)
        }

        private fun browsableItem(
            id: String,
            title: String,
            subtitle: String? = null,
            browsable: Boolean = true,
            artWorkUri: Uri? = null,
            type: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        ) = MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsPlayable(false)
                    .setIsBrowsable(browsable)
                    .setMediaType(type)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setArtworkUri(artWorkUri)
                    .build()
            )
            .build()

        private fun Track.toItem(
            context: Context, extensionId: String, con: EchoMediaItem? = null
        ): MediaItem {
            context.saveToCache(id, Triple(this, extensionId, con), "auto")
            return MediaItem.Builder()
                .setMediaId("auto/$id")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setTitle(title)
                        .setArtist(toMediaItem().subtitleWithE)
                        .setAlbumTitle(album?.title)
                        .setArtworkUri(cover?.toUri(context))
                        .build()
                ).build()
        }

        private suspend fun Extension<*>.toMediaItem(context: Context) = browsableItem(
            "$ROOT/$id", name, context.getString(R.string.extension),
            instance.value().isSuccess,
            metadata.icon?.toUri(context)
        )

        @OptIn(UnstableApi::class)
        val notSupported =
            LibraryResult.ofError<ImmutableList<MediaItem>>(SessionError.ERROR_NOT_SUPPORTED)

        @OptIn(UnstableApi::class)
        val errorIo = LibraryResult.ofError<ImmutableList<MediaItem>>(SessionError.ERROR_IO)

        suspend inline fun <reified C> Extension<*>.get(
            block: C.() -> List<MediaItem>
        ): LibraryResult<ImmutableList<MediaItem>> = runCatching {
            val client = instance.value().getOrThrow() as? C ?: return@runCatching notSupported
            LibraryResult.ofItemList(
                client.block(),
                MediaLibraryService.LibraryParams.Builder()
                    .setOffline(client is OfflineExtension)
                    .build()
            )
        }.getOrElse {
            it.printStackTrace()
            errorIo
        }


        private val itemMap = WeakHashMap<String, EchoMediaItem>()
        private fun EchoMediaItem.toMediaItem(
            context: Context, extId: String
        ): MediaItem = when (this) {
            is EchoMediaItem.TrackItem -> track.toItem(context, extId)
            else -> {
                val id = "${hashCode()}"
                itemMap[id] = this
                val (page, type) = when (this) {
                    is EchoMediaItem.Profile.ArtistItem -> ARTIST to MediaMetadata.MEDIA_TYPE_ARTIST
                    is EchoMediaItem.Profile.UserItem -> USER to MediaMetadata.MEDIA_TYPE_MIXED
                    is EchoMediaItem.Lists.AlbumItem -> ALBUM to MediaMetadata.MEDIA_TYPE_ALBUM
                    is EchoMediaItem.Lists.PlaylistItem -> PLAYLIST to MediaMetadata.MEDIA_TYPE_PLAYLIST
                    is EchoMediaItem.Lists.RadioItem -> RADIO to MediaMetadata.MEDIA_TYPE_RADIO_STATION
                    else -> throw IllegalStateException("Invalid type")
                }
                browsableItem(
                    "$ROOT/$extId/$page/$id",
                    title,
                    subtitleWithE,
                    true,
                    cover?.toUri(context),
                    type
                )
            }
        }

        private val listsMap = WeakHashMap<String, Shelf.Lists<*>>()
        private fun getListsItems(
            context: Context, id: String, extId: String
        ) = run {
            val shelf = listsMap[id]!!
            when (shelf) {
                is Shelf.Lists.Categories -> shelf.list.map { it.toMediaItem(context, extId) }
                is Shelf.Lists.Items -> shelf.list.map { it.toMediaItem(context, extId) }
                is Shelf.Lists.Tracks -> shelf.list.map { it.toItem(context, extId) }
            } + listOfNotNull(
                if (shelf.more != null) {
                    val moreId = "${shelf.more.hashCode()}"
                    shelvesMap[moreId] = when (shelf) {
                        is Shelf.Lists.Items -> shelf.more!!.map {
                            it.getOrThrow().map { item -> item.toShelf() }
                        }

                        is Shelf.Lists.Tracks -> shelf.more!!.map {
                            it.getOrThrow().map { track -> track.toMediaItem().toShelf() }
                        }

                        is Shelf.Lists.Categories -> shelf.more!!.map {
                            it.getOrThrow().map { category -> category }
                        }
                    }
                    browsableItem(
                        "$ROOT/$extId/$SHELF/$moreId",
                        context.getString(R.string.more)
                    )
                } else null
            )
        }

        private fun Shelf.toMediaItem(
            context: Context, extId: String
        ): MediaItem = when (this) {
            is Shelf.Category -> {
                val items = items
                val id = "${items.hashCode()}"
                if (items != null) shelvesMap[id] = items
                browsableItem("$ROOT/$extId/$SHELF/$id", title, subtitle, items != null)
            }

            is Shelf.Item -> media.toMediaItem(context, extId)
            is Shelf.Lists<*> -> {
                val id = "${hashCode()}"
                listsMap[id] = this
                browsableItem("$ROOT/$extId/$LIST/$id", title, subtitle)
            }
        }


        // THIS PROBABLY BREAKS GOING BACK TBH, NEED TO TEST
        private val shelvesMap = WeakHashMap<String, PagedData<Shelf>>()
        private val continuations = WeakHashMap<Pair<String, Int>, String?>()
        private suspend fun getShelfItems(
            context: Context, id: String, extId: String, page: Int
        ): List<MediaItem> {
            val shelf = shelvesMap[id]!!
            val (list, next) = shelf.loadList(continuations[id to page])
            continuations[id to page + 1] = next
            return listOfNotNull(
                *list.map { it.toMediaItem(context, extId) }.toTypedArray()
            )
        }

        private suspend fun PagedData<Shelf>.toMediaItems(
            context: Context, extId: String, page: Int
        ): List<MediaItem> {
            val id = "${hashCode()}"
            shelvesMap[id] = this
            return getShelfItems(context, id, extId, page)
        }

        private suspend inline fun <reified T> Extension<*>.getFeed(
            context: Context,
            parentId: String,
            page: String,
            pageNumber: Int,
            getTabs: T.() -> List<Tab>,
            getFeed: T.(tab: Tab?) -> PagedData<Shelf>
        ) = get<T> {
            val tabId = parentId.substringAfter("$page/").substringBefore("/")
            if (tabId.isNotBlank()) {
                val tab = Tab(tabId, tabId)
                getFeed(tab).toMediaItems(context, id, pageNumber)
            } else {
                val tabs = getTabs()
                if (tabs.isEmpty()) getFeed(null).toMediaItems(context, id, pageNumber)
                else tabs.map { browsableItem("$ROOT/$id/$page/${it.id}", it.title) }
            }
        }

        private val tracksMap = WeakHashMap<String, Pair<EchoMediaItem, PagedData<Track>>>()
        private suspend fun getTracks(
            context: Context,
            id: String,
            page: Int,
            getTracks: suspend () -> Pair<EchoMediaItem, PagedData<Track>>
        ): List<MediaItem> {
            val (item, tracks) = tracksMap.getOrPut(id) {
                val tracks = getTracks()
                tracksMap[id] = tracks
                tracks
            }
            val (list, next) = tracks.loadList(continuations[id to page])
            continuations[id to page + 1] = next
            return list.map { it.toItem(context, id, item) }
        }
    }

}