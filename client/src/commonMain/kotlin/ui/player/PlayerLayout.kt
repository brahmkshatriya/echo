package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet


private data object BlockBringIntoViewElement : ModifierNodeElement<BlockBringIntoViewNode>() {
    override fun create() = BlockBringIntoViewNode()

    override fun update(node: BlockBringIntoViewNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "blockDescendantBringIntoView"
    }
}

private class BlockBringIntoViewNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) = Unit
}

internal fun Modifier.blockDescendantBringIntoView(): Modifier = then(BlockBringIntoViewElement)

internal fun Modifier.clipHeaderTop(
    lazyListState: LazyListState,
    key: Any,
) = drawWithContent {
    val layoutInfo = lazyListState.layoutInfo
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
    val clipTop = item?.let { layoutInfo.beforeContentPadding - it.offset } ?: 0
    if (clipTop <= 0) drawContent()
    else clipRect(top = clipTop.toFloat().coerceAtMost(size.height)) {
        this@drawWithContent.drawContent()
    }
}

fun Modifier.expandedListItemTransform(
    playerSheet: BetterSheet?,
    playerHeight: () -> Int
): Modifier {
    return graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        val offset = 1 - positiveProgress
        alpha = if (positiveProgress > 0.5f) (positiveProgress - 0.5f) * 2 else 0f
        translationY = offset * playerHeight() * 0.33f
    }
}
