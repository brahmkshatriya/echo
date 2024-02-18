package dev.brahmkshatriya.localextenzion

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItemsContainer
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.data.offline.LocalAlbum
import dev.brahmkshatriya.echo.common.data.offline.LocalArtist
import dev.brahmkshatriya.echo.common.data.offline.LocalStream
import dev.brahmkshatriya.echo.common.data.offline.LocalTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class OfflineExtenzion : _root_ide_package_.dev.brahmkshatriya.echo.common.clients.ExtensionClient(), SearchClient, TrackClient, HomeFeedClient {

    override fun getMetadata() = ExtensionMetadata(
        name = "Offline",
        version = "1.0.0",
        description = "Local media library",
        author = "Echo",
        iconUrl = null
    )

    override suspend fun quickSearch(query: String): List<QuickSearchItem> = listOf()

    override suspend fun search(query: String): Flow<PagingData<MediaItemsContainer>> = flow {
        val trimmed = query.trim()
        val albums = LocalAlbum.search(context, trimmed, 1, 50)
            .map { it.toMediaItem() }.ifEmpty { null }
        val tracks = LocalTrack.search(context, trimmed, 1, 50)
            .map { it.toMediaItem() }.ifEmpty { null }
        val artists = LocalArtist.search(context, trimmed, 1, 50)
            .map { it.toMediaItem() }.ifEmpty { null }

        val result = listOfNotNull(
            tracks?.toMediaItemsContainer("Tracks"),
            albums?.toMediaItemsContainer("Albums"),
            artists?.toMediaItemsContainer("Artists")
        )
        emit(PagingData.from(result))
    }

    class OfflinePagingSource(val context: Context) : PagingSource<Int, MediaItemsContainer>() {
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
                val items = if (page == 0) {
                    val albums = LocalAlbum.getAll(context, page, pageSize)
                        .map { it.toMediaItem() }.ifEmpty { null }
                    val tracks = LocalTrack.getShuffled(context, page, pageSize)
                        .map { it.toMediaItem() }.ifEmpty { null }
                    val artists = LocalArtist.getAll(context, page, pageSize)
                        .map { it.toMediaItem() }.ifEmpty { null }
                    val result = listOfNotNull(
                        tracks?.toMediaItemsContainer("Tracks"),
                        albums?.toMediaItemsContainer("Albums"),
                        artists?.toMediaItemsContainer("Artists")
                    )
                    result
                } else {
                    LocalTrack.getAll(context, page, pageSize)
                        .map { MediaItemsContainer.TrackItem(it) }
                }
                val nextKey =
                    if (items.isEmpty()) null
                    else if (page == 0) 1
                    else page + 1

                LoadResult.Page(
                    data = items,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = nextKey
                )
            } catch (exception: IOException) {
                return LoadResult.Error(exception)
            }
        }
    }

    override suspend fun getTrack(uri: Uri): Track? {
        return LocalTrack.get(context, uri)
    }

    override suspend fun getStreamable(track: Track): StreamableAudio {
        return LocalStream.getFromTrack(context, track)?.toAudio()
            ?: throw IOException("Track not found")
    }

    override suspend fun getHomeGenres(): List<String> = listOf()

    override suspend fun getHomeFeed(genre: String?) = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { OfflinePagingSource(context) }
    ).flow


}