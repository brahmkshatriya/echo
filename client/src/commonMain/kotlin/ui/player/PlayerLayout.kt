package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.node.invalidateDrawForSubtree
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.unit.IntOffset
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import kotlin.math.roundToInt


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

fun Modifier.blockDescendantBringIntoView(): Modifier = then(BlockBringIntoViewElement)

/** Refresh cached descendant layers when retained player content is shown again. */
fun Modifier.redrawPlayerOnReveal(visible: Boolean): Modifier =
    then(PlayerRevealElement(visible))

private data class PlayerRevealElement(val visible: Boolean) : ModifierNodeElement<PlayerRevealNode>() {
    override fun create() = PlayerRevealNode(visible)

    override fun update(node: PlayerRevealNode) = node.update(visible)

    override fun InspectorInfo.inspectableProperties() {
        name = "redrawPlayerOnReveal"
        properties["visible"] = visible
    }
}

private class PlayerRevealNode(private var visible: Boolean) : Modifier.Node(),
    GlobalPositionAwareModifierNode, UnplacedAwareModifierNode {
    private var redrawAfterPlacement = true

    override fun onAttach() {
        redrawAfterPlacement = true
    }

    fun update(visible: Boolean) {
        val revealed = visible && !this.visible
        this.visible = visible
        if (revealed && isAttached) {
            redrawAfterPlacement = true
            // A retained page can become visible through layer properties alone.
            invalidateDrawForSubtree()
        }
    }

    override fun onUnplaced() {
        redrawAfterPlacement = true
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (visible && redrawAfterPlacement) {
            redrawAfterPlacement = false
            // Prefetch can record layers before their descendants are placed.
            invalidateDrawForSubtree()
        }
    }
}

/** Keep scroll fades on the scroll clock and exclude invisible controls from hit testing. */
fun Modifier.playerScrollFade(opacity: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        val alpha = opacity().coerceIn(0f, 1f)
        if (alpha > 0f) placeable.placeRelativeWithLayer(0, 0) { this.alpha = alpha }
    }
}

fun Modifier.clipHeaderTop(
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
    // Placement offsets let Compose move the subtree's bounds together. A
    // graphics-layer translation recalculates every descendant's transformed
    // bounds, even though none of their sizes or relative positions changed.
    return offset {
        val progress = (playerSheet?.progressState?.value ?: 0f).coerceIn(0f, 1f)
        IntOffset(0, ((1f - progress) * playerHeight() * 0.33f).roundToInt())
    }.graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.value ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        alpha = if (positiveProgress > 0.5f) (positiveProgress - 0.5f) * 2 else 0f
    }
}
