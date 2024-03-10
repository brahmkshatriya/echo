package dev.brahmkshatriya.echo.data.extensions

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException

abstract class PagedFlow<T : Any>(
    val pageSize: Int = 10
) {
    abstract fun loadItems(page: Int, pageSize: Int): List<T>

    fun getFlow() = Pager(
        config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
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
                        val items = loadItems(page, pageSize)
                        val nextKey = if (items.isEmpty()) null
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
        }).flow
}