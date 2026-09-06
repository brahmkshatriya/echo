package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CardParams(
    val modifier: Modifier,
    val key: Any,
    val contentType: Any,
    val colors: CardColors?,
    val elevation: CardElevation?,
    val border: BorderStroke?,
    val content: @Composable ColumnScope.() -> Unit,
)

fun LazyListScope.materialGroup(
    lazyListState: LazyListState,
    roundedCornerRadius: Dp = 22.dp,
    gap: Dp = 0.dp,
    clipPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    ignoredStickyHeaderKeys: Set<Any> = emptySet(),
    content: MaterialGroupScope.() -> Unit,
) {
    require(gap >= 0.dp) { "Material group gap must be non-negative" }
    val scope = MaterialGroupScope(
        radius = roundedCornerRadius,
        gap = gap,
        clipPadding = clipPadding,
        lazyListState = lazyListState,
        lazyListScope = this,
        reverseLayout = reverseLayout,
        ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
    )

    scope.content()
    scope.emit()
}

class MaterialGroupScope(
    private val radius: Dp,
    private val gap: Dp,
    private val clipPadding: PaddingValues,
    private val lazyListState: LazyListState,
    private val lazyListScope: LazyListScope,
    private val reverseLayout: Boolean,
    private val ignoredStickyHeaderKeys: Set<Any>,
) {
    private val items = mutableListOf<CardParams>()

    fun card(
        modifier: Modifier = Modifier,
        key: Any,
        contentType: Any,
        colors: CardColors? = null,
        elevation: CardElevation? = null,
        border: BorderStroke? = null,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        items += CardParams(modifier, key, contentType, colors, elevation, border, content)
    }

    fun emit() {
        if (items.isEmpty()) return
        val lastIndex = items.size - 1
        val gapCornerRadius = minOf(radius, gap * 2f)
        val itemKeys = items.mapTo(mutableSetOf()) { it.key }
        val firstKey = items.first().key
        val lastKey = items.last().key
        items.forEachIndexed { index, params ->
            lazyListScope.item(params.key, params.contentType) {
                val layoutDirection = LocalLayoutDirection.current
                val density = LocalDensity.current
                val clipPaddingPx = with(density) {
                    MaterialGroupClipPadding(
                        left = clipPadding.calculateLeftPadding(layoutDirection).roundToPx(),
                        top = clipPadding.calculateTopPadding().roundToPx(),
                        right = clipPadding.calculateRightPadding(layoutDirection).roundToPx(),
                        bottom = clipPadding.calculateBottomPadding().roundToPx()
                    )
                }
                Card(
                    modifier = params.modifier
                        .padding(
                            top = when {
                                reverseLayout && index == lastIndex -> clipPadding.calculateTopPadding()
                                reverseLayout -> 0.dp
                                index == 0 -> clipPadding.calculateTopPadding()
                                else -> 0.dp
                            },
                            bottom = when {
                                reverseLayout && index == 0 -> clipPadding.calculateBottomPadding()
                                reverseLayout -> gap
                                index != lastIndex -> gap
                                else -> clipPadding.calculateBottomPadding()
                            },
                        )
                        .clipToRoundedViewport(
                            lazyListState = lazyListState,
                            params = params,
                            clipPadding = clipPaddingPx,
                            radius = radius,
                            itemKeys = itemKeys,
                            firstKey = firstKey,
                            lastKey = lastKey,
                            reverseLayout = reverseLayout,
                            ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
                        ),
                    colors = params.colors ?: CardDefaults.cardColors(),
                    elevation = params.elevation ?: CardDefaults.cardElevation(),
                    border = params.border,
                    shape = if (gap > 0.dp) {
                        RoundedCornerShape(
                            topStart = if (if (reverseLayout) index < lastIndex else index > 0) gapCornerRadius else 0.dp,
                            topEnd = if (if (reverseLayout) index < lastIndex else index > 0) gapCornerRadius else 0.dp,
                            bottomStart = if (if (reverseLayout) index > 0 else index < lastIndex) gapCornerRadius else 0.dp,
                            bottomEnd = if (if (reverseLayout) index > 0 else index < lastIndex) gapCornerRadius else 0.dp,
                        )
                    } else {
                        RectangleShape
                    }
                ) {
                    params.content(this)
                }
            }
        }
    }
}

private data class MaterialGroupClipPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

private fun materialGroupClipInfo(
    lazyListState: LazyListState,
    params: CardParams,
    clipPadding: MaterialGroupClipPadding,
    itemKeys: Set<Any>,
    firstKey: Any,
    lastKey: Any,
    reverseLayout: Boolean,
    ignoredStickyHeaderKeys: Set<Any>
): Pair<Int?, Int?> {
    val layoutInfo = lazyListState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val currentItem = visibleItems.firstOrNull { it.key == params.key }
        ?: return null to null

    fun physicalOffset(itemOffset: Int, itemSize: Int): Int =
        if (reverseLayout) {
            layoutInfo.viewportSize.height - itemOffset - itemSize
        } else {
            itemOffset
        }

    val stickyHeaderItem = visibleItems.asSequence().filter { item ->
        item.index < currentItem.index &&
                item.key !in itemKeys &&
                item.key !in ignoredStickyHeaderKeys
    }.maxByOrNull { item -> item.index }
    val viewportStart = if (reverseLayout) {
        layoutInfo.viewportSize.height - layoutInfo.viewportEndOffset
    } else {
        layoutInfo.viewportStartOffset
    }
    val viewportEnd = if (reverseLayout) {
        layoutInfo.viewportSize.height - layoutInfo.viewportStartOffset
    } else {
        layoutInfo.viewportEndOffset
    }
    val stickyHeaderStart = stickyHeaderItem?.let { physicalOffset(it.offset, it.size) }
    val stickyHeaderEnd = stickyHeaderItem?.let {
        physicalOffset(it.offset, it.size) + it.size
    }
    val topGroupKey = if (reverseLayout) lastKey else firstKey
    val bottomGroupKey = if (reverseLayout) firstKey else lastKey
    val topGroupItem = visibleItems.firstOrNull { it.key == topGroupKey }
    val bottomGroupItem = visibleItems.firstOrNull { it.key == bottomGroupKey }
    val groupTop = topGroupItem?.let { physicalOffset(it.offset, it.size) }
    val groupBottom = bottomGroupItem?.let {
        physicalOffset(it.offset, it.size) + it.size
    }
    val visibleGroupTop = maxOf(
        if (reverseLayout) viewportStart else stickyHeaderEnd ?: viewportStart,
        groupTop ?: viewportStart,
    )
    val visibleGroupBottom = minOf(
        if (reverseLayout) stickyHeaderStart ?: viewportEnd else viewportEnd,
        groupBottom ?: viewportEnd,
    )
    val currentItemOffset = physicalOffset(currentItem.offset, currentItem.size)
    val top =
        visibleGroupTop - currentItemOffset + clipPadding.top
    val bottom =
        visibleGroupBottom - currentItemOffset - clipPadding.bottom
    return top to bottom
}

private fun Modifier.clipToRoundedViewport(
    lazyListState: LazyListState,
    params: CardParams,
    clipPadding: MaterialGroupClipPadding,
    radius: Dp,
    itemKeys: Set<Any>,
    firstKey: Any,
    lastKey: Any,
    reverseLayout: Boolean,
    ignoredStickyHeaderKeys: Set<Any>
): Modifier = drawWithCache {
    val path = Path()
    onDrawWithContent {
        val (topBoundary, bottomBoundary) = materialGroupClipInfo(
            lazyListState = lazyListState,
            params = params,
            clipPadding = clipPadding,
            itemKeys = itemKeys,
            firstKey = firstKey,
            lastKey = lastKey,
            reverseLayout = reverseLayout,
            ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
        )
        val hasNoBoundaries = topBoundary == null && bottomBoundary == null
        if (hasNoBoundaries) return@onDrawWithContent drawContent()

        val radiusPx = radius.toPx()
        val top = topBoundary?.toFloat() ?: (-radiusPx)
        val bottom = bottomBoundary?.toFloat() ?: (size.height + radiusPx)
        if (top >= bottom) return@onDrawWithContent

        val doesNotAffectItem = top <= -radiusPx && bottom >= size.height + radiusPx
        if (doesNotAffectItem) return@onDrawWithContent drawContent()
        val roundedViewportHeight = bottom - top
        val visibleTop = top.coerceIn(0f, size.height)
        val visibleBottom = bottom.coerceIn(0f, size.height)
        if (visibleBottom - visibleTop <= 0f) return@onDrawWithContent

        val cornerRadius = if (roundedViewportHeight > radiusPx) radiusPx
        else minOf(radiusPx, roundedViewportHeight / 2f, size.width / 2f)

        path.reset()
        path.addRoundRect(
            RoundRect(
                left = clipPadding.left.toFloat(),
                top = top,
                right = size.width - clipPadding.right,
                bottom = bottom,
                topLeftCornerRadius = CornerRadius(cornerRadius),
                topRightCornerRadius = CornerRadius(cornerRadius),
                bottomLeftCornerRadius = CornerRadius(cornerRadius),
                bottomRightCornerRadius = CornerRadius(cornerRadius),
            )
        )
        clipPath(path) {
            this@onDrawWithContent.drawContent()
        }
    }
}.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val (topBoundary, bottomBoundary) = materialGroupClipInfo(
                lazyListState = lazyListState,
                params = params,
                clipPadding = clipPadding,
                itemKeys = itemKeys,
                firstKey = firstKey,
                lastKey = lastKey,
                reverseLayout = reverseLayout,
                ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
            )
            val isOutOfBounds = event.changes.any {
                val isAbove = topBoundary?.let { top -> it.position.y < top } == true
                val isBelow = bottomBoundary?.let { bottom -> it.position.y > bottom } == true
                isAbove || isBelow
            }
            if (isOutOfBounds) event.changes.forEach { it.consume() }
        }
    }
}
