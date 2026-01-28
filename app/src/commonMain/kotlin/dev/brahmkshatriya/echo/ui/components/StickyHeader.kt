package dev.brahmkshatriya.echo.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

/**
 * Provides a layout that allows for placing a sticky header above the [content] Composable.
 *
 * @param state The [LazyListState] whose scroll properties will be observed to create a
 * sticky header for.
 * @param modifier The modifier to be applied to the layout.
 * @param isStickyHeaderItem A lambda for identifying which items in the list are sticky.
 * @param stickyHeader A lambda for drawing the sticky header composable. It also receives the
 * [LazyListItemInfo.key] and [LazyListItemInfo.contentType] for the item in the grid that the
 * sticky header is currently drawing over.
 * @param content The content the sticky header will be drawn over. This should be a [LazyColumn].
 */
@Composable
fun StickyHeaderList(
    state: LazyListState,
    modifier: Modifier = Modifier,
    isStickyHeaderItem: @DisallowComposableCalls (LazyListItemInfo) -> Boolean,
    stickyHeader: @Composable (index: Int, key: Any?, contentType: Any?) -> Unit,
    content: @Composable () -> Unit,
) {
    StickyHeaderLayout(
        lazyStateFunction = { state },
        modifier = modifier,
        itemMutationPolicy = {
            object : SnapshotMutationPolicy<LazyListItemInfo?> {
                override fun equivalent(
                    a: LazyListItemInfo?,
                    b: LazyListItemInfo?,
                ): Boolean =
                    a != null && b != null &&
                            a.key == b.key &&
                            a.contentType == b.contentType &&
                            a.index == b.index
            }
        },
        viewportStart = { layoutInfo.viewportStartOffset },
        lazyItems = { layoutInfo.visibleItemsInfo },
        lazyItemIndex = { index },
        lazyItemOffset = { offset },
        lazyItemHeight = { size },
        isStickyHeaderItem = isStickyHeaderItem,
        stickyHeader = { itemInfo ->
            if (itemInfo != null) stickyHeader(
                itemInfo.index,
                itemInfo.key,
                itemInfo.contentType
            )
        },
        content = content,
    )
}

@Composable
internal inline fun <LazyState : ScrollableState, LazyItem> StickyHeaderLayout(
    noinline lazyStateFunction: () -> LazyState,
    modifier: Modifier = Modifier,
    noinline itemMutationPolicy: () -> SnapshotMutationPolicy<LazyItem?>,
    crossinline viewportStart: @DisallowComposableCalls LazyState.() -> Int,
    crossinline lazyItems: @DisallowComposableCalls LazyState.() -> List<LazyItem>,
    crossinline lazyItemIndex: @DisallowComposableCalls LazyItem.() -> Int,
    crossinline lazyItemOffset: @DisallowComposableCalls LazyItem.() -> Int,
    crossinline lazyItemHeight: @DisallowComposableCalls LazyItem.() -> Int,
    crossinline isStickyHeaderItem: @DisallowComposableCalls LazyItem.() -> Boolean,
    stickyHeader: @Composable (LazyItem?) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.clipToBounds()) {
        content()
        var headerOffset by remember { mutableIntStateOf(Int.MIN_VALUE) }
        val lazyState = remember(lazyStateFunction) { lazyStateFunction() }
        LaunchedEffect(lazyState) {
            snapshotFlow {
                val startOffset = lazyState.viewportStart()
                val visibleItems = lazyState.lazyItems()
                val firstItem = visibleItems.firstOrNull()
                    ?: return@snapshotFlow Int.MIN_VALUE

                val firstItemIndex = firstItem.lazyItemIndex()
                val firstItemOffset = firstItem.lazyItemOffset()

                // The first item hast scrolled to the top of the view port yet, show nothing
                if (firstItemIndex == 0 && firstItemOffset > startOffset)
                    return@snapshotFlow Int.MIN_VALUE

                val firstCompletelyVisibleItem = visibleItems.firstOrNull { lazyItem ->
                    lazyItemOffset(lazyItem) >= startOffset
                } ?: return@snapshotFlow Int.MIN_VALUE

                when (isStickyHeaderItem(firstCompletelyVisibleItem)) {
                    false -> 0
                    true -> firstCompletelyVisibleItem.lazyItemHeight()
                        .minus(firstCompletelyVisibleItem.lazyItemOffset() - startOffset)
                        .let { difference -> if (difference < 0) 0 else -difference }
                }
            }
                .collect { headerOffset = it }
        }
        val canShowStickyHeader by remember {
            derivedStateOf(
                policy = structuralEqualityPolicy(),
                calculation = { headerOffset > Int.MIN_VALUE }
            )
        }
        Box(
//            modifier = Modifier.offset {
//                IntOffset(
//                    x = 0,
//                    y = headerOffset
//                )
//            }
        ) {
            val firstVisibleItem by remember(itemMutationPolicy) {
                derivedStateOf(
                    policy = itemMutationPolicy(),
                    calculation = { lazyState.lazyItems().firstOrNull() }
                )
            }
            if (canShowStickyHeader) stickyHeader(firstVisibleItem)
        }
    }
}