package dev.brahmkshatriya.echo.ui.paging

import dev.brahmkshatriya.echo.common.helpers.PagedData

fun <T : Any> PagedData<T>.toFlow() = when (this) {
    is PagedData.Single -> SingleSource({ loadList() }, { clear() }).toFlow()
    is PagedData.Continuous ->
        ContinuationSource<T, String>({ loadList(it) }, { invalidate(it) }).toFlow()
}