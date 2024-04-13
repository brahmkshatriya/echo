package dev.brahmkshatriya.echo.data.offline

import androidx.paging.PagingConfig
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.helpers.ErrorPagingSource
import dev.brahmkshatriya.echo.common.helpers.PagedData

class PagedSource<T : Any>(
    private val load: suspend (page: Int, pageSize: Int) -> List<T>,
) : ErrorPagingSource<Int, T>() {

    override val config: PagingConfig =
        PagingConfig(pageSize = 10, enablePlaceholders = false, initialLoadSize = 10)

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun loadData(params: LoadParams<Int>): LoadResult.Page<Int, T> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val items = load(page, pageSize)
        val nextKey = if (items.isEmpty()) null
        else page + 1
        return LoadResult.Page(
            data = items,
            prevKey = if (page == 0) null else page - 1,
            nextKey = nextKey
        )
    }

    companion object{
        fun <T : Any> pagedFlow(
            load: suspend (page: Int, pageSize: Int) -> List<T>
        ) = PagedData.Source(PagedSource(load))
    }
}