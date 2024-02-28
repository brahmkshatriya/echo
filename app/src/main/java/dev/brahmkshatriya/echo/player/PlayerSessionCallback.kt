package dev.brahmkshatriya.echo.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.paging.AsyncPagingDataDiffer
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus


class PlayerSessionCallback(
    private val context: Context,
    extensionFlow: Flow<ExtensionClient?>
) : MediaLibraryService.MediaLibrarySession.Callback {

    private val scope = CoroutineScope(Dispatchers.IO) + Job()

    init {
        scope.observe(extensionFlow) {
            extension = it
        }
    }

    private var extension: ExtensionClient? = null


    private val differ = AsyncPagingDataDiffer(
        MediaItemsContainerAdapter,
        MediaItemsContainerAdapter.ListCallback(),
    )

    private val handler = Handler(Looper.getMainLooper())
    private fun toast(message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {

        //Look at the first item's search query, if it's null, return the super method
        val query = mediaItems.firstOrNull()?.requestMetadata?.searchQuery
            ?: return super.onAddMediaItems(mediaSession, controller, mediaItems)

        fun default(reason: String): ListenableFuture<MutableList<MediaItem>> {
            toast(reason)
            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }

        val extension = extension
            ?: return default("Extension isn't loaded.")
        if (extension !is SearchClient)
            return default("Extension does not support Searching")
        if (extension !is TrackClient)
            return default("Extension does not support Streaming Tracks")


        return scope.future {
            differ.submitData(extension.search(query).first())
            val list = differ.snapshot().items.map {
                when (it) {
                    is MediaItemsContainer.Category -> {
                        it.list.mapNotNull { item ->
                            if (item is EchoMediaItem.TrackItem) {
                                val track = item.track
                                val stream = extension.getStreamable(track)
                                Global.addTrack(scope, track, stream).second
                            } else null
                        }
                    }

                    is MediaItemsContainer.TrackItem -> {
                        val track = it.track
                        val stream = extension.getStreamable(track)
                        listOf(Global.addTrack(scope, track, stream).second)
                    }
                }
            }.flatten()
            if (list.isEmpty())
                return@future default("Couldn't find anything related to $query").get()
            list.toMutableList()
        }
    }

    @UnstableApi
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        Global.clearQueue(scope)
        return super.onSetMediaItems(
            mediaSession,
            controller,
            mediaItems,
            startIndex,
            startPositionMs
        )
    }
    //CAN BE USED FOR ANDROID AUTO SUPPORT, BUT IDK I DONT WANT TO ADD IT RN
    //HALF OF IT IS KANGED FROM INNERTUNE

//
//    override fun onGetLibraryRoot(
//        session: MediaLibraryService.MediaLibrarySession,
//        browser: MediaSession.ControllerInfo,
//        params: MediaLibraryService.LibraryParams?
//    ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
//        LibraryResult.ofItem(
//            MediaItem.Builder()
//                .setMediaId(ROOT)
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setIsPlayable(false)
//                        .setIsBrowsable(false)
//                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
//                        .build()
//                )
//                .build(),
//            params
//        )
//    ).also {
//        println("onGetLibraryRoot")
//    }
//
//    override fun onGetChildren(
//        session: MediaLibraryService.MediaLibrarySession,
//        browser: MediaSession.ControllerInfo,
//        parentId: String,
//        page: Int,
//        pageSize: Int,
//        params: MediaLibraryService.LibraryParams?,
//    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
//        scope.future(Dispatchers.IO) {
//            if (extension !is HomeFeedClient) LibraryResult.ofItemList(emptyList(), params)
//            else
//                LibraryResult.ofItemList(
//                    when (parentId) {
//                        ROOT -> {
//                            extension.getHomeFeed(null).collectLatest {
//                                differ.submitData(it)
//                            }
//                            listOf(
//                                browsableMediaItem(
//                                    SONG,
//                                    context.getString(R.string.tracks),
//                                    drawableUri(R.drawable.ic_heart_filled_40dp),
//                                    MediaMetadata.MEDIA_TYPE_PLAYLIST
//                                ),
//                                browsableMediaItem(
//                                    ARTIST,
//                                    context.getString(R.string.artists),
//                                    drawableUri(R.drawable.ic_more_horiz),
//                                    MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS
//                                ),
//                                browsableMediaItem(
//                                    ALBUM,
//                                    context.getString(R.string.albums),
//                                    drawableUri(R.drawable.ic_home_filled),
//                                    MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
//                                ),
//                                browsableMediaItem(
//                                    PLAYLIST,
//                                    context.getString(R.string.playlists),
//                                    drawableUri(R.drawable.ic_repeat_40dp),
//                                    MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
//                                )
//                            )
//                        }
//
//                        SONG -> {
//                            if (extension is TrackClient) {
//                                differ.snapshot().items.map {
//                                    when (it) {
//                                        is MediaItemsContainer.Category -> it.list.mapNotNull {
//                                            if (it is EchoMediaItem.TrackItem)
//                                                PlayerHelper.mediaItemBuilder(
//                                                    it.track,
//                                                    extension.getStreamable(it.track)
//                                                )
//                                            else null
//                                        }
//
//                                        is MediaItemsContainer.TrackItem -> listOf(
//                                            PlayerHelper.mediaItemBuilder(
//                                                it.track,
//                                                extension.getStreamable(it.track)
//                                            )
//                                        )
//                                    }
//                                }.flatten()
//                            } else emptyList()
//                        }
//
//                        ARTIST -> {
//                            differ.snapshot().items.map {
//                                if (it is MediaItemsContainer.Category) {
//                                    it.list.mapNotNull {
//                                        if (it is EchoMediaItem.ArtistItem) browsableMediaItem(
//                                            "${ARTIST}/${it.artist.uri}",
//                                            it.artist.name,
//                                            null,
//                                            MediaMetadata.MEDIA_TYPE_ARTIST
//                                        )
//                                        else null
//                                    }
//                                } else emptyList()
//                            }.flatten()
//                        }
//
//                        ALBUM -> differ.snapshot().items.map {
//                            if (it is MediaItemsContainer.Category) {
//                                it.list.mapNotNull {
//                                    if (it is EchoMediaItem.AlbumItem) browsableMediaItem(
//                                        "${ALBUM}/${it.album.uri}",
//                                        it.album.title,
//                                        null,
//                                        MediaMetadata.MEDIA_TYPE_ALBUM
//                                    )
//                                    else null
//                                }
//                            } else emptyList()
//                        }.flatten()
//
//                        else -> when {
//                            parentId.startsWith("${ARTIST}/") -> {
//                                if (extension is ArtistClient) {
//                                    val artist = extension.loadArtist(
//                                        Artist.Small(
//                                            parentId.removePrefix("${ARTIST}/").toUri(), ""
//                                        )
//                                    )
//                                    extension.getMediaItems(artist).collectLatest {
//                                        differ.submitData(it)
//                                    }
//                                    emptyList()
//                                } else emptyList()
//                            }
//
//                            parentId.startsWith("${ALBUM}/") -> {
//                                if (extension is AlbumClient && extension is TrackClient) {
//                                    val album = extension.loadAlbum(
//                                        Album.Small(
//                                            parentId.removePrefix("${ALBUM}/").toUri(), ""
//                                        )
//                                    )
//                                    album.tracks.map {
//                                        PlayerHelper.mediaItemBuilder(
//                                            it,
//                                            extension.getStreamable(it)
//                                        )
//                                    }
//                                } else emptyList()
//                            }
//
//                            else -> emptyList()
//                        }
//                    },
//                    params
//                )
//        }.also {
//            println("onGetChildren")
//        }


//    private fun browsableMediaItem(
//        id: String,
//        title: String,
//        iconUri: Uri?,
//        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC
//    ) =
//        MediaItem.Builder()
//            .setMediaId(id)
//            .setMediaMetadata(
//                MediaMetadata.Builder()
//                    .setTitle(title)
//                    .setArtworkUri(iconUri)
//                    .setIsPlayable(false)
//                    .setIsBrowsable(true)
//                    .setMediaType(mediaType)
//                    .build()
//            )
//            .build()
//
//    private fun drawableUri(@DrawableRes id: Int) = Uri.Builder()
//        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
//        .authority(context.resources.getResourcePackageName(id))
//        .appendPath(context.resources.getResourceTypeName(id))
//        .appendPath(context.resources.getResourceEntryName(id))
//        .build()
//
//    private fun <T> notSupported() = Futures.immediateFuture(
//        LibraryResult.ofError<T>(LibraryResult.RESULT_ERROR_NOT_SUPPORTED)
//    )


//    override fun onSearch(
//        session: MediaLibraryService.MediaLibrarySession,
//        browser: MediaSession.ControllerInfo,
//        query: String,
//        params: MediaLibraryService.LibraryParams?
//    ): ListenableFuture<LibraryResult<Void>> {
//        if (extension !is SearchClient) return notSupported()
//        println("onSearch")
//        return scope.future(Dispatchers.IO) {
//            Log.d("BRUH", query)
//            launch {
//                extension.search(query).collectLatest {
//                    differ.submitData(it)
//                }
//            }
//            delay(1000)
//            val list = differ.snapshot().items.map {
//                val track = (it as? MediaItemsContainer.TrackItem)?.track ?: return@map null
//                Log.d("BRUH", track.toString())
//                track
//            }.filterNotNull()
//            session.notifySearchResultChanged(browser, query, list.count(), params)
//            LibraryResult.ofVoid(params)
//        }
//    }
//
//    override fun onGetSearchResult(
//        session: MediaLibraryService.MediaLibrarySession,
//        browser: MediaSession.ControllerInfo,
//        query: String,
//        page: Int,
//        pageSize: Int,
//        params: MediaLibraryService.LibraryParams?
//    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
//        println("onGetSearchResult")
//        if (extension !is TrackClient) return notSupported()
//        if (extension !is SearchClient) return notSupported()
//
//        return scope.future(Dispatchers.IO) {
//            Log.d("BRUH", query)
//            launch {
//                extension.search(query).collectLatest {
//                    differ.submitData(it)
//                }
//            }
//            delay(1000)
//            val list = differ.snapshot().items.map {
//                val track = (it as? MediaItemsContainer.TrackItem)?.track ?: return@map null
//                val stream = extension.getStreamable(track)
//                PlayerHelper.mediaItemBuilder(track, stream)
//            }.filterNotNull()
//            LibraryResult.ofItemList(list, params)
//        }
//    }
//
//    override fun onGetItem(
//        session: MediaLibraryService.MediaLibrarySession,
//        browser: MediaSession.ControllerInfo,
//        mediaId: String
//    ): ListenableFuture<LibraryResult<MediaItem>> {
//        println("onGetItem")
//        if (extension !is TrackClient) return notSupported()
//        return scope.future(Dispatchers.IO) {
//            val uri = mediaId.removePrefix("${SONG}/").toUri()
//            val track = extension.getTrack(uri)
//                ?: return@future LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
//            val stream = extension.getStreamable(track)
//            val item = PlayerHelper.mediaItemBuilder(track, stream)
//            LibraryResult.ofItem(item, null)
//        }
//    }
//
//    companion object {
//        const val ROOT = "root"
//        const val SONG = "song"
//        const val ARTIST = "artist"
//        const val ALBUM = "album"
//        const val PLAYLIST = "playlist"
//    }
}