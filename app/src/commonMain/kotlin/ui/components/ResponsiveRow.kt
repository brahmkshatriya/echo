package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

interface ResponsiveRowScope {
    fun item(
        priority: Int,
        key: Any? = null,
        weight: Float = 0f,
        content: @Composable () -> Unit,
    )

    fun spacer(
        whenItemHidden: Any,
        key: Any? = null,
        weight: Float = 1f,
    )
}

private data class ResponsiveRowItem(
    val priority: Int,
    val key: Any,
    val weight: Float,
    val whenItemHidden: Any? = null,
    val content: @Composable () -> Unit,
)

private class ResponsiveRowScopeImpl : ResponsiveRowScope {
    val items = mutableListOf<ResponsiveRowItem>()

    override fun item(
        priority: Int,
        key: Any?,
        weight: Float,
        content: @Composable () -> Unit,
    ) {
        require(weight >= 0f) { "weight must be >= 0" }
        items += ResponsiveRowItem(
            priority = priority,
            key = key ?: items.size,
            weight = weight,
            content = content,
        )
    }

    override fun spacer(
        whenItemHidden: Any,
        key: Any?,
        weight: Float,
    ) {
        require(weight >= 0f) { "weight must be >= 0" }
        items += ResponsiveRowItem(
            priority = Int.MIN_VALUE,
            key = key ?: items.size,
            weight = weight,
            whenItemHidden = whenItemHidden,
            content = {},
        )
    }
}

private class ResponsiveRowItemState {
    val visibility = Animatable(0f)
    var targetVisible: Boolean? = null
    var visibleWidth = 0
}

/**
 * A row that automatically keeps the highest-priority set of items that fits its width.
 *
 * Items are considered in descending [ResponsiveRowScope.item] priority, but are always placed in
 * declaration order. Non-weighted items use their preferred intrinsic width; weighted items use
 * their minimum intrinsic width and split whatever width remains after the fitting pass.
 * Conditional [ResponsiveRowScope.spacer] entries can consume leftover width only while another
 * keyed item is hidden.
 */
@Composable
fun ResponsiveRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    animationSpec: FiniteAnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
    ),
    content: ResponsiveRowScope.() -> Unit,
) {
    val scope = ResponsiveRowScopeImpl().apply(content)
    val items = scope.items
    val keys = items.map { it.key }
    require(keys.distinct().size == keys.size) { "ResponsiveRow item keys must be unique" }

    val states = remember(keys) { List(items.size) { ResponsiveRowItemState() } }
    val coroutineScope = rememberCoroutineScope()

    Layout(
        contents = items.map { item ->
            @Composable {
                Box { item.content() }
            }
        },
        modifier = modifier,
    ) { measurableGroups, constraints ->
        if (items.isEmpty()) {
            return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        }

        val measurables = measurableGroups.map { group ->
            require(group.size == 1) { "ResponsiveRow items must produce one root layout" }
            group.single()
        }
        val boundedWidth = constraints.maxWidth != Constraints.Infinity
        val availableWidth = if (boundedWidth) constraints.maxWidth else Int.MAX_VALUE / 4
        val spacingPx = spacing.roundToPx()
        val intrinsicHeight = if (constraints.maxHeight == Constraints.Infinity) 0 else constraints.maxHeight
        val intrinsicWidths = IntArray(items.size) { index ->
            val measurable = measurables[index]
            val width = if (items[index].weight > 0f) {
                measurable.minIntrinsicWidth(intrinsicHeight)
            } else {
                measurable.maxIntrinsicWidth(intrinsicHeight)
            }
            width.coerceAtLeast(0).coerceAtMost(availableWidth)
        }
        val targetVisible = BooleanArray(items.size)
        var usedWidth = 0
        var visibleWithWidth = 0
        val priorityOrder = items.indices
            .filter { items[it].whenItemHidden == null }
            .sortedWith(compareByDescending<Int> { items[it].priority }.thenBy { it })

        priorityOrder.forEach { index ->
            val itemWidth = intrinsicWidths[index]
            val addsSpacing = itemWidth > 0 && visibleWithWidth > 0
            val requiredWidth = itemWidth + if (addsSpacing) spacingPx else 0
            if (!boundedWidth || usedWidth + requiredWidth <= availableWidth) {
                targetVisible[index] = true
                usedWidth += requiredWidth
                if (itemWidth > 0) visibleWithWidth++
            }
        }

        val indexByKey = items.indices.associateBy { items[it].key }
        items.indices.forEach { index ->
            val hiddenKey = items[index].whenItemHidden ?: return@forEach
            val dependencyIndex = indexByKey[hiddenKey]
                ?: error("ResponsiveRow spacer references unknown item key: $hiddenKey")
            targetVisible[index] = !targetVisible[dependencyIndex]
        }

        val selectedWeight = items.indices.sumOf { index ->
            if (targetVisible[index]) items[index].weight.toDouble() else 0.0
        }.toFloat()
        var selectedBaseWidth = 0
        var selectedWithWidth = 0
        items.indices.forEach { index ->
            if (!targetVisible[index]) return@forEach
            val itemWidth = intrinsicWidths[index]
            val occupiesWidth = itemWidth > 0 || items[index].weight > 0f
            if (occupiesWidth && selectedWithWidth > 0) selectedBaseWidth += spacingPx
            selectedBaseWidth += itemWidth
            if (occupiesWidth) selectedWithWidth++
        }
        var remainingWidth = if (boundedWidth) {
            (availableWidth - selectedBaseWidth).coerceAtLeast(0)
        } else {
            0
        }
        val targetWidths = intrinsicWidths.copyOf()
        if (selectedWeight > 0f && remainingWidth > 0) {
            val weightedIndices = items.indices.filter {
                targetVisible[it] && items[it].weight > 0f
            }
            weightedIndices.forEachIndexed { weightedPosition, index ->
                val extra = if (weightedPosition == weightedIndices.lastIndex) {
                    remainingWidth
                } else {
                    ((availableWidth - selectedBaseWidth) * (items[index].weight / selectedWeight))
                        .roundToInt()
                        .coerceAtMost(remainingWidth)
                }
                targetWidths[index] += extra
                remainingWidth -= extra
            }
        }

        val progress = FloatArray(items.size)
        items.indices.forEach { index ->
            val state = states[index]
            val nextVisible = targetVisible[index]
            if (nextVisible) state.visibleWidth = targetWidths[index]

            val previousTarget = state.targetVisible
            progress[index] = if (previousTarget == null) {
                if (nextVisible) 1f else 0f
            } else {
                state.visibility.value
            }

            if (previousTarget == null) {
                state.targetVisible = nextVisible
                coroutineScope.launch {
                    state.visibility.snapTo(if (nextVisible) 1f else 0f)
                }
            } else if (previousTarget != nextVisible) {
                state.targetVisible = nextVisible
                coroutineScope.launch {
                    state.visibility.animateTo(
                        targetValue = if (nextVisible) 1f else 0f,
                        animationSpec = animationSpec,
                    )
                }
            }
        }

        val measureWidths = IntArray(items.size) { index ->
            when {
                targetVisible[index] -> targetWidths[index]
                states[index].visibleWidth > 0 -> states[index].visibleWidth
                else -> intrinsicWidths[index]
            }.coerceIn(0, availableWidth)
        }
        val placeables = measurables.mapIndexed { index, measurable ->
            val width = measureWidths[index]
            measurable.measure(
                Constraints(
                    minWidth = width,
                    maxWidth = width,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                )
            )
        }

        val currentWidths = IntArray(items.size) { index ->
            (measureWidths[index] * progress[index]).roundToInt()
        }
        val activeIndices = items.indices.filter { progress[it] > 0.001f && currentWidths[it] > 0 }
        val currentContentWidth = activeIndices.fold(0) { width, index ->
            width + currentWidths[index]
        } + spacingPx * (activeIndices.size - 1).coerceAtLeast(0)
        val layoutWidth = (if (boundedWidth) availableWidth else currentContentWidth)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val contentHeight = activeIndices.maxOfOrNull { placeables[it].height } ?: 0
        val layoutHeight = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            var x = 0
            activeIndices.forEachIndexed { activePosition, index ->
                val placeable = placeables[index]
                val itemProgress = progress[index]
                val y = verticalAlignment.align(placeable.height, layoutHeight)
                placeable.placeRelativeWithLayer(x, y) {
                    alpha = itemProgress
                    scaleX = itemProgress
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (layoutDirection == LayoutDirection.Ltr) 0f else 1f,
                        pivotFractionY = 0.5f,
                    )
                }
                x += currentWidths[index]
                if (activePosition != activeIndices.lastIndex) x += spacingPx
            }
        }
    }
}
