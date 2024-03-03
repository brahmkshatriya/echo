package dev.brahmkshatriya.echo.data.extensions

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItemsContainer
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.data.offline.AlbumResolver
import dev.brahmkshatriya.echo.data.offline.ArtistResolver
import dev.brahmkshatriya.echo.data.offline.TrackResolver
import dev.brahmkshatriya.echo.data.offline.sortedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class OfflineExtension(val context: Context) : ExtensionClient, SearchClient, TrackClient,
    HomeFeedClient, AlbumClient {

    override val metadata = ExtensionMetadata(
        name = "Offline",
        version = "1.0.0",
        description = "Local media library",
        author = "Echo",
        iconUrl = null
    )

    val artistResolver = ArtistResolver(context)
    val albumResolver = AlbumResolver(context)
    val trackResolver = TrackResolver(context)

    override suspend fun quickSearch(query: String): List<QuickSearchItem> = listOf()

    override suspend fun search(query: String): Flow<PagingData<MediaItemsContainer>> = flow {
        val trimmed = query.trim()
        val albums = albumResolver.search(trimmed, 1, 50).map { it.toMediaItem() }.toMutableList()
            .ifEmpty { null }
        val tracks = trackResolver.search(trimmed, 1, 50).map { it.toMediaItem() }.toMutableList()
            .ifEmpty { null }
        val artists = artistResolver.search(trimmed, 1, 50).map { it.toMediaItem() }.toMutableList()
            .ifEmpty { null }

        val exactMatch = artists?.firstOrNull { it.artist.name.equals(trimmed, true) }.also {
            artists?.remove(it)
        } ?: albums?.firstOrNull { it.album.title.equals(trimmed, true) }.also {
            albums?.remove(it)
        } ?: tracks?.firstOrNull { it.track.title.equals(trimmed, true) }.also {
            tracks?.remove(it)
        }
        val result: MutableList<MediaItemsContainer> = listOfNotNull(tracks?.let {
            val track = it.firstOrNull()?.track ?: return@let null
            track.title to it.toMediaItemsContainer("Tracks")
        }, albums?.let {
            val album = it.firstOrNull()?.album ?: return@let null
            album.title to it.toMediaItemsContainer("Albums")
        }, artists?.let {
            val artist = it.firstOrNull()?.artist ?: return@let null
            artist.name to it.toMediaItemsContainer("Artists")
        }).sortedBy(query) { it.first }.map { it.second }.toMutableList()
        exactMatch?.toMediaItemsContainer()?.let { result.add(0, it) }
        emit(PagingData.from(result))
    }

    inner class OfflinePagingSource(val context: Context, private val genre: StateFlow<String?>) :
        PagingSource<Int, MediaItemsContainer>() {
        override fun getRefreshKey(state: PagingState<Int, MediaItemsContainer>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
            }
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemsContainer> {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            return try {
                val items = when (genre.value) {
                    "Tracks" -> trackResolver.getAll(page, pageSize)
                        .map { MediaItemsContainer.TrackItem(it) }

                    "Albums" -> albumResolver.getAll(page, pageSize)
                        .map { MediaItemsContainer.AlbumItem(it) }

                    "Artists" -> artistResolver.getAll(page, pageSize)
                        .map { MediaItemsContainer.ArtistItem(it) }

                    else -> if (page == 0) {
                        val albums =
                            albumResolver.getShuffled(page, pageSize).map { it.toMediaItem() }
                                .ifEmpty { null }
                        val tracks =
                            trackResolver.getShuffled(page, pageSize).map { it.toMediaItem() }
                                .ifEmpty { null }
                        val artists =
                            artistResolver.getShuffled(page, pageSize).map { it.toMediaItem() }
                                .ifEmpty { null }
                        val result = listOfNotNull(
                            tracks?.toMediaItemsContainer("Tracks"),
                            albums?.toMediaItemsContainer("Albums"),
                            artists?.toMediaItemsContainer("Artists")
                        )
                        result
                    } else {
                        trackResolver.getAll(page, pageSize)
                            .map { MediaItemsContainer.TrackItem(it) }
                    }
                }
                val nextKey = if (items.isEmpty()) null
                else if (page == 0) 1
                else page + 1

                LoadResult.Page(
                    data = items, prevKey = if (page == 0) null else page - 1, nextKey = nextKey
                )
            } catch (exception: IOException) {
                return LoadResult.Error(exception)
            }
        }
    }

    override suspend fun getTrack(uri: Uri): Track? {
        return trackResolver.get(uri)
    }

    override suspend fun getStreamable(track: Track): StreamableAudio {
        return trackResolver.getStream(track).toAudio()
    }

    override suspend fun getHomeGenres(): List<String> {
        return listOf(
            "All", "Tracks", "Albums", "Artists"
        )
    }

    override suspend fun getHomeFeed(genre: StateFlow<String?>) =
        Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { OfflinePagingSource(context, genre) }).flow

    override suspend fun loadAlbum(small: Album.Small): Album.Full {
        return albumResolver.get(small.uri, trackResolver)
    }

    override suspend fun getMediaItems(album: Album.Full): Flow<PagingData<MediaItemsContainer>> =
        flow {
            val artist = album.artist.name
            val tracks = trackResolver.search(artist, 1, 50).filter { it.album?.uri != album.uri }
                .map { it.toMediaItem() }.ifEmpty { null }
            val albums = albumResolver.search(artist, 1, 50).filter { it.uri != album.uri }
                .map { it.toMediaItem() }.ifEmpty { null }
            val result = listOfNotNull(
                tracks?.toMediaItemsContainer("More from $artist"),
                albums?.toMediaItemsContainer("Albums")
            )
            emit(PagingData.from(result))
        }


}