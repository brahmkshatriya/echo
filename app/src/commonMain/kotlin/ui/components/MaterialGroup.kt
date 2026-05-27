package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
    clipPadding: PaddingValues = PaddingValues(0.dp),
    ignoredStickyHeaderKeys: Set<Any> = emptySet(),
    content: MaterialGroupScope.() -> Unit,
) {
    val scope = MaterialGroupScope(
        radius = roundedCornerRadius,
        clipPadding = clipPadding,
        lazyListState = lazyListState,
        lazyListScope = this,
        ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
    )

    scope.content()
    scope.emit()
}

class MaterialGroupScope(
    private val radius: Dp,
    private val clipPadding: PaddingValues,
    private val lazyListState: LazyListState,
    private val lazyListScope: LazyListScope,
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
                            top = if (index == 0) clipPadding.calculateTopPadding() else 0.dp,
                            bottom = if (index == lastIndex) clipPadding.calculateBottomPadding() else 0.dp
                        )
                        .clipToRoundedViewport(
                            lazyListState = lazyListState,
                            params = params,
                            clipPadding = clipPaddingPx,
                            radius = radius,
                            itemKeys = itemKeys,
                            firstKey = firstKey,
                            lastKey = lastKey,
                            ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
                        ),
                    colors = params.colors ?: CardDefaults.cardColors(),
                    elevation = params.elevation ?: CardDefaults.cardElevation(),
                    border = params.border,
                    shape = RectangleShape
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
    ignoredStickyHeaderKeys: Set<Any>
): Pair<Int?, Int?> {
    val layoutInfo = lazyListState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val currentItem = visibleItems.firstOrNull { it.key == params.key }
        ?: return null to null

    val stickyHeaderItem = visibleItems.asSequence().filter { item ->
        item.index < currentItem.index &&
                item.key !in itemKeys &&
                item.key !in ignoredStickyHeaderKeys
    }.maxByOrNull { item -> item.index }
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val stickyHeaderBottom = stickyHeaderItem?.let { it.offset + it.size }
    val firstGroupItem = visibleItems.firstOrNull { it.key == firstKey }
    val lastGroupItem = visibleItems.firstOrNull { it.key == lastKey }
    val groupTop = firstGroupItem?.offset
    val groupBottom = lastGroupItem?.let { it.offset + it.size }
    val visibleGroupTop = maxOf(stickyHeaderBottom ?: viewportStart, groupTop ?: viewportStart)
    val visibleGroupBottom = minOf(groupBottom ?: viewportEnd, viewportEnd)
    val top =
        visibleGroupTop - currentItem.offset + clipPadding.top
    val bottom =
        visibleGroupBottom - currentItem.offset - clipPadding.bottom
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
    ignoredStickyHeaderKeys: Set<Any>
): Modifier {
    val (topBoundary, bottomBoundary) = materialGroupClipInfo(
        lazyListState = lazyListState,
        params = params,
        clipPadding = clipPadding,
        itemKeys = itemKeys,
        firstKey = firstKey,
        lastKey = lastKey,
        ignoredStickyHeaderKeys = ignoredStickyHeaderKeys
    )
    return drawWithContent {
        val hasNoBoundaries = topBoundary == null && bottomBoundary == null
        if (hasNoBoundaries) return@drawWithContent drawContent()

        val radiusPx = radius.toPx()
        val top = topBoundary?.toFloat() ?: (-radiusPx)
        val bottom = bottomBoundary?.toFloat() ?: (size.height + radiusPx)
        if (top >= bottom) return@drawWithContent

        val doesNotAffectItem = top <= -radiusPx && bottom >= size.height + radiusPx
        if (doesNotAffectItem) return@drawWithContent drawContent()
        val roundedViewportHeight = bottom - top
        val visibleTop = top.coerceIn(0f, size.height)
        val visibleBottom = bottom.coerceIn(0f, size.height)
        val visibleHeight = visibleBottom - visibleTop
        if (visibleHeight <= 0f) return@drawWithContent

        val radius = if (roundedViewportHeight > radiusPx) radiusPx
        else minOf(radiusPx, roundedViewportHeight / 2f, size.width / 2f)

        clipPath(Path().apply {
            addRoundRect(
                RoundRect(
                    left = clipPadding.left.toFloat(),
                    top = top,
                    right = size.width - clipPadding.right,
                    bottom = bottom,
                    topLeftCornerRadius = CornerRadius(radius),
                    topRightCornerRadius = CornerRadius(radius),
                    bottomLeftCornerRadius = CornerRadius(radius),
                    bottomRightCornerRadius = CornerRadius(radius),
                )
            )
        }) {
            this@drawWithContent.drawContent()
        }
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val isOutOfBounds = event.changes.any {
                    val isAbove = topBoundary?.let { top -> it.position.y < top } == true
                    val isBelow = bottomBoundary?.let { bottom -> it.position.y > bottom } == true
                    isAbove || isBelow
                }
                if (isOutOfBounds) event.changes.forEach { it.consume() }
            }
        }
    }
}
