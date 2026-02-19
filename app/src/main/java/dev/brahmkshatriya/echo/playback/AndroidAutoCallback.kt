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
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.pagedDataOfFirst
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.MediaState
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
    open val app: App,
    open val scope: CoroutineScope,
    open val extensionList: StateFlow<List<MusicExtension>>,
    open val downloadFlow: StateFlow<List<Downloader.Info>>
) : MediaLibrarySession.Callback {

    val context get() = app.context

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
            ALBUM -> extension.getList<AlbumClient> {
                val id = parentId.substringAfter("$ALBUM/").substringBefore("/")
                val unloaded = itemMap[id] as Album
                getTracks(context, id, page) {
                    val album = loadAlbum(unloaded)
                    album to loadTracks(album)
                }
            }

            PLAYLIST -> extension.getList<PlaylistClient> {
                val id = parentId.substringAfter("$PLAYLIST/").substringBefore("/")
                val unloaded = itemMap[id] as Playlist
                getTracks(context, id, page) {
                    val playlist = loadPlaylist(unloaded)
                    playlist to loadTracks(playlist)
                }
            }

            RADIO -> extension.getList<RadioClient> {
                val id = parentId.substringAfter("$RADIO/").substringBefore("/")
                val radio = itemMap[id] as Radio
                getTracks(context, id, page) {
                    radio to loadTracks(radio)
                }
            }

            ARTIST -> extension.getList<ArtistClient> {
                val id = parentId.substringAfter("$ARTIST/").substringBefore("/")
                val artist = loadArtist(Artist(id, ""))
                loadFeed(artist).toMediaItems(artist.id, context, extId, page)
            }

            LIST -> extension.getList<ExtensionClient> {
                val id = parentId.substringAfter("$LIST/").substringBefore("/")
                getListsItems(context, id, extId)
            }

            SHELF -> extension.getList<ExtensionClient> {
                val id = parentId.substringAfter("$SHELF/").substringBefore("/")
                getShelfItems(context, id, extId, page)
            }

            HOME -> extension.getFeed<HomeFeedClient>(
                context, parentId, HOME, page
            ) { loadHomeFeed() }

            LIBRARY -> extension.getFeed<LibraryFeedClient>(
                context, parentId, LIBRARY, page
            ) { loadLibraryFeed() }

            SEARCH -> extension.getFeed<SearchFeedClient>(
                context, parentId, SEARCH, page
            ) { loadSearchFeed(searchQuery) }

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
                MediaItemUtils.build(
                    app,
                    downloadFlow.value,
                    MediaState.Unloaded(extId, track),
                    con
                )
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
        private const val FEED = "feed"
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
            is ImageHolder.ResourceUriImageHolder -> uri.toUri()
            is ImageHolder.NetworkRequestImageHolder -> request.url.toUri()
            is ImageHolder.ResourceIdImageHolder -> context.resources.getUri(resId)
            is ImageHolder.HexColorImageHolder -> "".toUri()
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
                        .setArtist(subtitleWithE)
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

        suspend inline fun <reified C> Extension<*>.getList(
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
            is Track -> toItem(context, extId)
            else -> {
                val id = hashCode().toString()
                itemMap[id] = this
                val (page, type) = when (this) {
                    is Artist, is Radio -> USER to MediaMetadata.MEDIA_TYPE_MIXED
                    is Album -> ALBUM to MediaMetadata.MEDIA_TYPE_ALBUM
                    is Playlist -> PLAYLIST to MediaMetadata.MEDIA_TYPE_PLAYLIST
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
                    val moreId = shelf.id
                    feedMap[moreId] = shelf.more
                    browsableItem(
                        "$ROOT/$extId/$FEED/$moreId",
                        context.getString(R.string.more)
                    )
                } else null
            )
        }

        private fun Shelf.toMediaItem(
            context: Context, extId: String
        ): MediaItem = when (this) {
            is Shelf.Category -> {
                val items = feed
                if (items != null) feedMap[id] = items
                browsableItem("$ROOT/$extId/$FEED/$id", title, subtitle, items != null)
            }

            is Shelf.Item -> media.toMediaItem(context, extId)
            is Shelf.Lists<*> -> {
                val id = "${id.hashCode()}"
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
            val (list, next) = shelf.loadPage(continuations[id to page])
            continuations[id to page + 1] = next
            return listOfNotNull(
                *list.map { it.toMediaItem(context, extId) }.toTypedArray()
            )
        }

        private val feedMap = WeakHashMap<String, Feed<Shelf>>()
        private suspend fun Feed<Shelf>.toMediaItems(
            id: String, context: Context, extId: String, page: Int
        ): List<MediaItem> {
            val id = "${id.hashCode()}"
            feedMap[id] = this
            val shelf = shelvesMap.getOrPut(id) {
                this.pagedDataOfFirst()
            }
            val (list, next) = shelf.loadPage(continuations[id to page])
            continuations[id to page + 1] = next
            return listOfNotNull(
                *list.map { it.toMediaItem(context, extId) }.toTypedArray()
            )
        }

        private suspend inline fun <reified T> Extension<*>.getFeed(
            context: Context,
            parentId: String,
            page: String,
            pageNumber: Int,
            getFeed: T.() -> Feed<Shelf>
        ) = getList<T> {
            val feed = getFeed()
            feed.toMediaItems(parentId, context, id, pageNumber)
        }

        private val tracksMap = WeakHashMap<String, Pair<EchoMediaItem, PagedData<Track>>>()
        private suspend fun getTracks(
            context: Context,
            id: String,
            page: Int,
            getTracks: suspend () -> Pair<EchoMediaItem, Feed<Track>?>
        ): List<MediaItem> {
            val (item, tracks) = tracksMap.getOrPut(id) {
                val tracks = getTracks().run {
                    first to (second?.run { getPagedData(tabs.firstOrNull()) }?.pagedData
                        ?: PagedData.empty())
                }
                tracksMap[id] = tracks
                tracks
            }
            val (list, next) = tracks.loadPage(continuations[id to page])
            continuations[id to page + 1] = next
            return list.map { it.toItem(context, id, item) }
        }
    }

}