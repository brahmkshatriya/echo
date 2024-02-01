package dev.brahmkshatriya.echo.data.extensions

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.data.clients.SearchClient
import dev.brahmkshatriya.echo.data.models.MediaItem
import dev.brahmkshatriya.echo.data.models.QuickSearchItem
import dev.brahmkshatriya.echo.data.models.toMediaItem
import dev.brahmkshatriya.echo.data.offline.searchLocally
import kotlinx.coroutines.flow.Flow
import java.io.IOException

class OfflineExtension(val context:Context) : SearchClient {
    override suspend fun quickSearch(query: String): List<QuickSearchItem> = listOf()

    override suspend fun search(query: String): Map<String, Flow<PagingData<MediaItem>>> {
        return mapOf("Result" to toPagingSource { page, pageSize ->
            context.searchLocally(query, page, pageSize)
                .map { it.toMediaItem() }
        })
    }

    private fun <TData : Any> toPagingSource(dataCallback: suspend (Int, Int) -> List<TData>): Flow<PagingData<TData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SearchPagingSource(dataCallback) }
        ).flow
    }

    class SearchPagingSource<TData : Any>(
        private val dataRequest: suspend (Int, Int) -> List<TData>,
        private val startPage: Int = 1,
        private val pageSize: Int = 10
    )  : PagingSource<Int, TData>() {
        override fun getRefreshKey(state: PagingState<Int, TData>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
            }
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TData> {
            val pageNumber = params.key ?: startPage
            return try {
                val items = dataRequest(pageNumber, params.loadSize)
                val nextKey = if (items.isEmpty())
                    null
                else
                    pageNumber + (params.loadSize / pageSize)
                LoadResult.Page(
                    data = items,
                    prevKey = if (pageNumber == startPage) null else pageNumber - 1,
                    nextKey = nextKey
                )
            } catch (exception: IOException) {
                return LoadResult.Error(exception)
            }
        }
    }
}