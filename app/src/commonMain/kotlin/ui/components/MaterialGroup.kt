package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class CardParams(
    val modifier: Modifier,
    val key: Any?,
    val contentType: Any?,
    val colors: CardColors?,
    val elevation: CardElevation?,
    val border: BorderStroke?,
    val content: @Composable () -> Unit,
)

private data class MaterialGroupClipInfo(
    val topBoundary: Int?,
    val bottomBoundary: Int?,
)

class MaterialGroupScope internal constructor(
    private val radius: Dp,
    private val verticalPadding: Dp,
    private val lazyListState: LazyListState?,
    private val lazyListScope: LazyListScope,
) {
    private val zero = 0.dp
    private val items = mutableListOf<CardParams>()

    fun card(
        modifier: Modifier = Modifier,
        key: Any? = null,
        contentType: Any? = null,
        colors: CardColors? = null,
        elevation: CardElevation? = null,
        border: BorderStroke? = null,
        content: @Composable () -> Unit,
    ) {
        items += CardParams(modifier, key, contentType, colors, elevation, border, content)
    }

    internal fun emit() {
        val lastIndex = items.lastIndex
        val itemKeys = items.mapTo(mutableSetOf()) { it.key }
        val firstKey = items.firstOrNull()?.key
        val lastKey = items.lastOrNull()?.key
        items.forEachIndexed { index, params ->
            lazyListScope.item(params.key, params.contentType) {
                val isTop = index == 0
                val isBottom = index == lastIndex
                val clipInfo by remember(lazyListState, params.key) {
                    derivedStateOf {
                        materialGroupClipInfo(
                            state = lazyListState,
                            params = params,
                            itemKeys = itemKeys,
                            firstKey = firstKey,
                            lastKey = lastKey,
                        )
                    }
                }
                val topBoundary = clipInfo.topBoundary
                val bottomBoundary = clipInfo.bottomBoundary
                Card(
                    modifier = params.modifier.padding(
                        top = if (isTop) verticalPadding else zero,
                        bottom = if (isBottom) verticalPadding else zero
                    )
                        .clipToRoundedViewport(topBoundary, bottomBoundary, radius)
                        .blockClippedPointerInput(topBoundary, bottomBoundary),
                    colors = params.colors ?: CardDefaults.cardColors(),
                    elevation = params.elevation ?: CardDefaults.cardElevation(),
                    border = params.border,
                    shape = RoundedCornerShape(zero)
                ) {
                    params.content()
                }
            }
        }
    }
}

private fun materialGroupClipInfo(
    state: LazyListState?,
    params: CardParams,
    itemKeys: Set<Any?>,
    firstKey: Any?,
    lastKey: Any?,
): MaterialGroupClipInfo {
    if (state == null || params.key == null) {
        return MaterialGroupClipInfo(topBoundary = null, bottomBoundary = null)
    }

    val visibleItems = state.layoutInfo.visibleItemsInfo
    val currentItem = visibleItems.firstOrNull { it.key == params.key }
        ?: return MaterialGroupClipInfo(topBoundary = null, bottomBoundary = null)

    val stickyHeaderItem = visibleItems
        .asSequence()
        .filter { item ->
            item.index < currentItem.index && item.key !in itemKeys
        }
        .maxByOrNull { item -> item.index }
    val viewportStart = state.layoutInfo.viewportStartOffset
    val viewportEnd = state.layoutInfo.viewportEndOffset
    val stickyHeaderBottom = stickyHeaderItem?.let { it.offset + it.size }
    val firstGroupItem = visibleItems.firstOrNull { it.key == firstKey }
    val lastGroupItem = visibleItems.firstOrNull { it.key == lastKey }
    val groupTop = firstGroupItem?.offset
    val groupBottom = lastGroupItem?.let { it.offset + it.size }
    val visibleGroupTop = maxOf(stickyHeaderBottom ?: viewportStart, groupTop ?: viewportStart)
    val visibleGroupBottom = minOf(groupBottom ?: viewportEnd, viewportEnd)

    return MaterialGroupClipInfo(
        topBoundary = visibleGroupTop - currentItem.offset,
        bottomBoundary = visibleGroupBottom - currentItem.offset,
    )
}

@Composable
private fun Modifier.blockClippedPointerInput(topBoundary: Int?, bottomBoundary: Int?): Modifier {
    val currentTopBoundary by rememberUpdatedState(topBoundary)
    val currentBottomBoundary by rememberUpdatedState(bottomBoundary)
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any {
                        currentTopBoundary?.let { top -> it.position.y < top } == true ||
                                currentBottomBoundary?.let { bottom -> it.position.y > bottom } == true
                    }
                ) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}

private fun Modifier.clipToRoundedViewport(
    topBoundary: Int?,
    bottomBoundary: Int?,
    radius: Dp,
): Modifier = drawWithContent {
    if (topBoundary == null && bottomBoundary == null) {
        drawContent()
        return@drawWithContent
    }

    val radiusPx = radius.toPx()
    val top = topBoundary?.toFloat() ?: -radiusPx
    val bottom = bottomBoundary?.toFloat() ?: (size.height + radiusPx)
    if (top >= bottom) return@drawWithContent

    val roundedViewportHeight = bottom - top
    val visibleTop = top.coerceIn(0f, size.height)
    val visibleBottom = bottom.coerceIn(0f, size.height)
    val visibleHeight = visibleBottom - visibleTop
    if (visibleHeight <= 0f) return@drawWithContent

    val doesNotAffectItem = top <= -radiusPx &&
            bottom >= size.height + radiusPx
    if (doesNotAffectItem) {
        drawContent()
        return@drawWithContent
    }

    if (roundedViewportHeight <= radiusPx) {
        val visibleRadius = minOf(radiusPx, roundedViewportHeight / 2f, size.width / 2f)
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = top,
                    right = size.width,
                    bottom = bottom,
                    topLeftCornerRadius = CornerRadius(visibleRadius),
                    topRightCornerRadius = CornerRadius(visibleRadius),
                    bottomLeftCornerRadius = CornerRadius(visibleRadius),
                    bottomRightCornerRadius = CornerRadius(visibleRadius),
                )
            )
        }
        clipPath(path) {
            this@drawWithContent.drawContent()
        }
        return@drawWithContent
    }

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f,
                top = top,
                right = size.width,
                bottom = bottom,
                topLeftCornerRadius = CornerRadius(radiusPx),
                topRightCornerRadius = CornerRadius(radiusPx),
                bottomLeftCornerRadius = CornerRadius(radiusPx),
                bottomRightCornerRadius = CornerRadius(radiusPx),
            )
        )
    }
    clipPath(path) {
        this@drawWithContent.drawContent()
    }
}

fun LazyListScope.materialGroup(
    roundedCornerRadius: Dp = 22.dp,
    verticalPadding: Dp = 0.dp,
    lazyListState: LazyListState? = null,
    content: MaterialGroupScope.() -> Unit,
) {
    val scope = MaterialGroupScope(
        radius = roundedCornerRadius,
        verticalPadding = verticalPadding,
        lazyListState = lazyListState,
        lazyListScope = this
    )

    scope.content()
    scope.emit()
}
