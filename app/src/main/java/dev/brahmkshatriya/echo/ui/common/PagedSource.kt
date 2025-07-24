package dev.brahmkshatriya.echo.ui.common

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.helpers.PagedData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.flow

class PagedSource<T : Any>(
    private val result: Deferred<Result<PagedData<T>?>>
) : PagingSource<String, T>() {

    val flow = Pager(
        PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            prefetchDistance = 20
        )
    ) { this }.flow

    override val keyReuseSupported = true
    override fun getRefreshKey(state: PagingState<String, T>): String? {
        val pos = state.anchorPosition ?: return null
        val key = state.closestPageToPosition(pos)?.nextKey
        invalidate(key)
        return key
    }

    private var data: PagedData<T>? = null
    private fun invalidate(key: String?) {
        data?.invalidate(key)
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        val key = params.key
        val data = result.await().getOrElse {
            return LoadResult.Error(it)
        } ?: return LoadResult.Page(
            data = emptyList(), prevKey = null, nextKey = null
        )
        return runCatching {
            val page = data.loadList(key)
            LoadResult.Page(page.data, key, page.continuation)
        }.getOrElse {
            LoadResult.Error(it)
        }
    }

    companion object {
        fun <T : Any> emptyFlow() = flow {
            emit(
                PagingData.Companion.empty<T>(
                    LoadStates(
                        LoadState.Loading,
                        LoadState.NotLoading(false),
                        LoadState.NotLoading(true)
                    )
                )
            )
        }
    }
}