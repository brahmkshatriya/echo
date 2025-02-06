package dev.brahmkshatriya.echo.ui.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.helpers.Page

class ContinuationSource<C : Any>(
    private val load: suspend (token: String?) -> Page<C>,
    private val invalidate: (token: String?) -> Unit
) : ErrorPagingSource<String, C>() {

    override val config = PagingConfig(pageSize = 10, enablePlaceholders = false)
    override suspend fun loadData(params: LoadParams<String>): LoadResult.Page<String, C> {
        val token = params.key
        val page = load(token)
        return LoadResult.Page(
            data = page.data,
            prevKey = null,
            nextKey = page.continuation
        )
    }

    override fun getRefreshKey(state: PagingState<String, C>) = state.anchorPosition?.let { pos ->
        state.closestPageToPosition(pos)?.nextKey?.let { key ->
            invalidate(key)
            key
        }
    }
}