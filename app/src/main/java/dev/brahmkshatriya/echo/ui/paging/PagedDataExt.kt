package dev.brahmkshatriya.echo.ui.paging

import dev.brahmkshatriya.echo.common.helpers.PagedData

fun <T : Any> PagedData<T>.toFlow() = when (this) {
    is PagedData.Single -> SingleSource({ loadList() }, { clear() }).toFlow()
    else -> ContinuationSource({ loadList(it) }, { invalidate(it) }).toFlow()
}