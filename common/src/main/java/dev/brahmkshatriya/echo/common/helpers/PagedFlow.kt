package dev.brahmkshatriya.echo.common.helpers

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException

class PagedFlow<T : Any>(
    private val load : suspend (page: Int, pageSize: Int) -> List<T>
) {

    fun getFlow() = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false, initialLoadSize = 10),
        pagingSourceFactory = {
            object : PagingSource<Int, T>() {
                override fun getRefreshKey(state: PagingState<Int, T>): Int? {
                    return state.anchorPosition?.let { anchorPosition ->
                        state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                            ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
                    }
                }

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
                    val page = params.key ?: 0
                    val pageSize = params.loadSize
                    return try {
                        val items = load(page, pageSize)
                        val nextKey = if (items.isEmpty()) null
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
        }).flow
}