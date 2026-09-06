package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import dev.brahmkshatriya.echo.app.platform.VariableText
import echo.app.generated.resources.GoogleSansFlex
import echo.app.generated.resources.Res
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.Font

private val TopAppBarHorizontalPadding = 4.dp
private val TopAppBarTitleInset = 12.dp
private val MediumTitleBottomPadding = 24.dp
private const val ExpandedTitleWidthAxis = 125f
private const val CollapsedTitleWidthAxis = 100f

private class ScaledTitleLines(var value: List<String>)

private class CollapsedTitleCache(
    var width: Int = -1,
    var visibleEnd: Int = 0,
    var text: String = "",
)

private fun resolvedTitleLines(
    text: String,
    layout: androidx.compose.ui.text.TextLayoutResult,
): List<String> = buildList {
    for (lineIndex in 0 until layout.lineCount) {
        val start = layout.getLineStart(lineIndex).coerceIn(0, text.length)
        val end = layout.getLineEnd(lineIndex, visibleEnd = true).coerceIn(start, text.length)
        add(text.substring(start, end).trimEnd())
    }
}

private fun ellipsizedTitlePrefix(text: String, visibleEnd: Int): String {
    if (visibleEnd >= text.length) return text
    if (visibleEnd <= 0) return "…"

    var safeEnd = visibleEnd.coerceIn(0, text.length)
    if (
        safeEnd in 1 until text.length &&
        text[safeEnd].isLowSurrogate() &&
        text[safeEnd - 1].isHighSurrogate()
    ) {
        safeEnd--
    }
    return text.substring(0, safeEnd).trimEnd().trimEnd('…') + "…"
}

private fun resolveCollapsedTitle(
    text: String,
    maxWidth: Int,
    style: androidx.compose.ui.text.TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): Pair<Int, String> {
    if (text.isEmpty() || maxWidth <= 0) return 0 to ""

    val fullWidth = textMeasurer.measure(
        text = text,
        style = style,
        softWrap = false,
        maxLines = 1,
        constraints = Constraints(maxWidth = Constraints.Infinity),
    ).size.width
    if (fullWidth <= maxWidth) return text.length to text

    var low = 0
    var high = text.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        val candidate = ellipsizedTitlePrefix(text, mid)
        val candidateWidth = textMeasurer.measure(
            text = candidate,
            style = style,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(maxWidth = Constraints.Infinity),
        ).size.width
        if (candidateWidth <= maxWidth) {
            low = mid
        } else {
            high = mid - 1
        }
    }
    return low to ellipsizedTitlePrefix(text, low)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaledTopAppBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    collapsedHeight: Dp = TopAppBarDefaults.MediumAppBarCollapsedHeight,
    expandedHeight: Dp = TopAppBarDefaults.MediumAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val resolvedCollapsedHeight =
        if (!collapsedHeight.isSpecified || !collapsedHeight.isFinite) {
            TopAppBarDefaults.MediumAppBarCollapsedHeight
        } else {
            collapsedHeight
        }
    val resolvedExpandedHeight =
        if (!expandedHeight.isSpecified || !expandedHeight.isFinite) {
            TopAppBarDefaults.MediumAppBarExpandedHeight
        } else {
            expandedHeight
        }
    require(resolvedExpandedHeight >= resolvedCollapsedHeight) {
        "The expandedHeight is expected to be greater or equal to the collapsedHeight"
    }

    val expandedTitleStyle = typography.titleLarge
    val collapsedTitleStyle = typography.titleMedium
    val expandedSubtitleStyle = typography.bodyMedium
    val collapsedSubtitleStyle = typography.bodySmall
    val collapsedTitleWeight = collapsedTitleStyle.fontWeight?.weight
        ?: FontWeight.Medium.weight
    val collapsedTitleScale =
        collapsedTitleStyle.fontSize.value / expandedTitleStyle.fontSize.value
    val collapsedSubtitleScale =
        collapsedSubtitleStyle.fontSize.value / expandedSubtitleStyle.fontSize.value
    val collapsedTitleFontWeight = FontWeight(collapsedTitleWeight)
    val expandedVariationSettings = remember {
        FontVariation.Settings(
            FontVariation.Setting("wght", 800f),
            FontVariation.Setting("wdth", ExpandedTitleWidthAxis),
        )
    }
    val collapsedVariationSettings = remember(collapsedTitleWeight) {
        FontVariation.Settings(
            FontVariation.Setting("wght", collapsedTitleWeight.toFloat()),
            FontVariation.Setting("wdth", CollapsedTitleWidthAxis),
        )
    }
    val expandedMeasurementFont = Font(
        Res.font.GoogleSansFlex,
        weight = FontWeight.ExtraBold,
        variationSettings = expandedVariationSettings,
    )
    val collapsedMeasurementFont = Font(
        Res.font.GoogleSansFlex,
        weight = collapsedTitleFontWeight,
        variationSettings = collapsedVariationSettings,
    )
    val expandedMeasurementFontFamily = remember(expandedMeasurementFont) {
        FontFamily(expandedMeasurementFont)
    }
    val collapsedMeasurementFontFamily = remember(collapsedMeasurementFont) {
        FontFamily(collapsedMeasurementFont)
    }
    val textMeasurer = rememberTextMeasurer()
    val expandedTitleMeasureStyle = remember(expandedMeasurementFontFamily, expandedTitleStyle) {
        expandedTitleStyle.copy(
            fontFamily = expandedMeasurementFontFamily,
            fontWeight = FontWeight.ExtraBold,
        )
    }
    val collapsedTitleMeasureStyle = remember(
        collapsedMeasurementFontFamily,
        collapsedTitleStyle,
        collapsedTitleFontWeight,
    ) {
        collapsedTitleStyle.copy(
            fontFamily = collapsedMeasurementFontFamily,
            fontWeight = collapsedTitleFontWeight,
        )
    }
    val titleLayout = remember(title, expandedTitleMeasureStyle, textMeasurer) {
        textMeasurer.measure(
            text = title,
            style = expandedTitleMeasureStyle,
            maxLines = 1,
            softWrap = false,
            constraints = Constraints(maxWidth = Constraints.Infinity),
        )
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val titleBaseline = remember(titleLayout) {
        floatArrayOf(titleLayout.getLineBaseline(0))
    }
    val titleX = remember(title) { floatArrayOf(0f) }
    val titleLines = remember(title) { ScaledTitleLines(listOf(title)) }
    val collapsedTitleCache = remember(title) { CollapsedTitleCache() }
    val collapseFraction = {
        (scrollBehavior?.state?.collapsedFraction ?: 0f).coerceIn(0f, 1f)
    }
    val insetPadding = windowInsets.asPaddingValues()

    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx =
            if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val horizontalPaddingPx = with(density) { TopAppBarHorizontalPadding.roundToPx() }
        val titleInsetPx = with(density) { TopAppBarTitleInset.roundToPx() }
        val mediumTitleBottomPaddingPx = with(density) { MediumTitleBottomPadding.roundToPx() }
        val collapsedHeightPx = with(density) { resolvedCollapsedHeight.roundToPx() }
        val minimumExpandedHeightPx = with(density) { resolvedExpandedHeight.roundToPx() }
        val titleLineHeightPx = with(density) {
            if (expandedTitleStyle.lineHeight.isSp) {
                expandedTitleStyle.lineHeight.toPx()
            } else {
                titleLayout.size.height.toFloat()
            }
        }
        val startInsetPx = with(density) {
            insetPadding.calculateStartPadding(layoutDirection).roundToPx()
        }
        val endInsetPx = with(density) {
            insetPadding.calculateEndPadding(layoutDirection).roundToPx()
        }
        val expandedInnerWidth =
            (containerWidthPx - startInsetPx - endInsetPx).coerceAtLeast(0)
        val expandedMaxTitleWidth =
            (expandedInnerWidth -
                    titleInsetPx -
                    horizontalPaddingPx -
                    2 * horizontalPaddingPx)
                .coerceAtLeast(0)
        val expandedTitleMeasure = textMeasurer.measure(
            text = title,
            style = expandedTitleMeasureStyle,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            constraints = Constraints(maxWidth = expandedMaxTitleWidth),
        )
        val expandedSubtitleMeasure = subtitle?.let {
            textMeasurer.measure(
                text = it,
                style = expandedSubtitleStyle,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = expandedMaxTitleWidth),
            )
        }
        val expandedRenderedTitleHeight =
            (titleLineHeightPx * expandedTitleMeasure.lineCount).roundToInt()
        val expandedLastBaseline =
            if (expandedSubtitleMeasure != null) {
                expandedRenderedTitleHeight +
                    expandedSubtitleMeasure.getLineBaseline(0).roundToInt()
            } else {
                ((titleBaseline.firstOrNull() ?: 0f) +
                        (expandedTitleMeasure.lineCount - 1) * titleLineHeightPx)
                    .roundToInt()
            }
        val expandedGroupHeight =
            expandedRenderedTitleHeight + (expandedSubtitleMeasure?.size?.height ?: 0)
        val expandedBottomPadding = max(
            0,
            mediumTitleBottomPaddingPx - (expandedGroupHeight - expandedLastBaseline),
        )
        val effectiveExpandedHeightPx = max(
            minimumExpandedHeightPx,
            collapsedHeightPx + expandedGroupHeight + expandedBottomPadding,
        )
        val collapseRangePx = (effectiveExpandedHeightPx - collapsedHeightPx).toFloat()

        SideEffect {
            scrollBehavior?.state?.let { state ->
                val limit = -collapseRangePx
                state.heightOffsetLimit = limit
                state.heightOffset = state.heightOffset.coerceIn(limit, 0f)
            }
        }

        val dragState = rememberDraggableState { delta ->
            scrollBehavior?.state?.let { state ->
                state.heightOffset += delta
            }
        }
        val dragModifier =
            if (scrollBehavior != null && !scrollBehavior.isPinned) {
                Modifier.draggable(
                    orientation = Orientation.Vertical,
                    state = dragState,
                    onDragStopped = { velocity ->
                        settleScaledTopAppBar(
                            state = scrollBehavior.state,
                            velocity = velocity,
                            flingAnimationSpec = scrollBehavior.flingAnimationSpec,
                            snapAnimationSpec = scrollBehavior.snapAnimationSpec,
                        )
                    },
                )
            } else {
                Modifier
            }

        val containerColor = {
            lerp(
                colors.containerColor,
                colors.scrolledContainerColor,
                FastOutLinearInEasing.transform(collapseFraction()),
            )
        }

        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragModifier)
                .drawBehind { drawRect(containerColor()) }
                .semantics { isTraversalGroup = true }
                .pointerInput(Unit) {}
                .clipToBounds(),
        content = {
            Box(
                modifier = Modifier
                    .layoutId("navigationIcon")
                    .padding(start = TopAppBarHorizontalPadding),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.navigationIconContentColor,
                ) {
                    navigationIcon?.invoke()
                }
            }

            VariableText(
                text = title,
                peakPosition = { null },
                glyphXPositions = titleX,
                glyphBaselines = titleBaseline,
                color = colors.titleContentColor,
                fontSize = expandedTitleStyle.fontSize,
                uniformWeight = {
                    val progress = collapseFraction()
                    800f + (collapsedTitleWeight - 800f) * progress
                },
                uniformWidth = {
                    ExpandedTitleWidthAxis +
                        (CollapsedTitleWidthAxis - ExpandedTitleWidthAxis) * collapseFraction()
                },
                fontScale = { 1f + (collapsedTitleScale - 1f) * collapseFraction() },
                scaleFromBottom = false,
                uniformLines = { titleLines.value },
                lineHeight = expandedTitleStyle.lineHeight,
                modifier = Modifier.layoutId("title"),
            )

            subtitle?.let {
                Text(
                    text = it,
                    modifier = Modifier.layoutId("subtitle"),
                    maxLines = 1,
                    style = expandedSubtitleStyle,
                    color = colors.subtitleContentColor,
                )
            }

            Box(
                modifier = Modifier
                    .layoutId("actionIcons")
                    .padding(end = TopAppBarHorizontalPadding),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.actionIconContentColor,
                ) {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions?.invoke(this)
                    }
                }
            }
        },
        ) { measurables, constraints ->
        val startInsetPx = insetPadding.calculateStartPadding(layoutDirection).roundToPx()
        val endInsetPx = insetPadding.calculateEndPadding(layoutDirection).roundToPx()
        val topInsetPx = insetPadding.calculateTopPadding().roundToPx()
        val bottomInsetPx = insetPadding.calculateBottomPadding().roundToPx()
        val layoutWidth =
            if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val innerWidth = (layoutWidth - startInsetPx - endInsetPx).coerceAtLeast(0)

        val collapsedRowConstraints = Constraints(
            minWidth = 0,
            maxWidth = innerWidth,
            minHeight = 0,
            maxHeight = collapsedHeightPx,
        )
        val navigationPlaceable = measurables
            .first { it.layoutId == "navigationIcon" }
            .measure(collapsedRowConstraints)
        val actionIconsPlaceable = measurables
            .first { it.layoutId == "actionIcons" }
            .measure(collapsedRowConstraints)

        // These are the same start/end calculations used by Material3's TopAppBarLayout.
        // navigationPlaceable/actionIconsPlaceable already include the 4.dp edge padding above.
        val collapsedTitleStartSpace = max(titleInsetPx, navigationPlaceable.width)
        val collapsedMaxTitleWidth =
            (innerWidth -
                    collapsedTitleStartSpace -
                    actionIconsPlaceable.width -
                    2 * horizontalPaddingPx)
                .coerceAtLeast(0)
        val measureProgress = collapseFraction()
        val maxTitleWidth =
            (expandedMaxTitleWidth +
                    (collapsedMaxTitleWidth - expandedMaxTitleWidth) * measureProgress)
                .roundToInt()

        val expandedTitleX = startInsetPx + titleInsetPx + horizontalPaddingPx
        val collapsedTitleX =
            startInsetPx + collapsedTitleStartSpace + horizontalPaddingPx

        if (collapsedTitleCache.width != collapsedMaxTitleWidth) {
            val (visibleEnd, collapsedText) = resolveCollapsedTitle(
                text = title,
                maxWidth = collapsedMaxTitleWidth,
                style = collapsedTitleMeasureStyle,
                textMeasurer = textMeasurer,
            )
            collapsedTitleCache.width = collapsedMaxTitleWidth
            collapsedTitleCache.visibleEnd = visibleEnd
            collapsedTitleCache.text = collapsedText
        }
        val collapsedVisibleEnd = collapsedTitleCache.visibleEnd
        val isExpanded = measureProgress <= 0.0001f
        val isCollapsed = measureProgress >= 0.999f
        val visibleTitleEnd =
            (title.length + (collapsedVisibleEnd - title.length) * measureProgress)
                .roundToInt()
                .coerceIn(collapsedVisibleEnd, title.length)
        val visibleTitle = when {
            isExpanded -> title
            isCollapsed -> collapsedTitleCache.text
            else -> ellipsizedTitlePrefix(title, visibleTitleEnd)
        }
        val currentTitleScale = 1f + (collapsedTitleScale - 1f) * measureProgress
        val currentTitleWidthAxis =
            ExpandedTitleWidthAxis +
                (CollapsedTitleWidthAxis - ExpandedTitleWidthAxis) * measureProgress
        val measurementToRenderedWidthScale =
            (currentTitleScale * currentTitleWidthAxis / ExpandedTitleWidthAxis)
                .coerceAtLeast(0.001f)
        val currentLogicalTitleWidth =
            (maxTitleWidth / measurementToRenderedWidthScale)
                .roundToInt()
                .coerceAtLeast(0)
        val currentTitleLines = when {
            isExpanded -> resolvedTitleLines(title, expandedTitleMeasure)
            isCollapsed -> listOf(collapsedTitleCache.text)
            else -> {
                val currentTitleMeasure = textMeasurer.measure(
                    text = visibleTitle,
                    style = expandedTitleMeasureStyle,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    constraints = Constraints(maxWidth = currentLogicalTitleWidth),
                )
                resolvedTitleLines(visibleTitle, currentTitleMeasure)
            }
        }
        titleLines.value = currentTitleLines
        val currentTitleHeight =
            (titleLineHeightPx * currentTitleLines.size * currentTitleScale)
                .roundToInt()
                .coerceAtLeast(1)

        val titlePlaceable = measurables
            .first { it.layoutId == "title" }
            .measure(
                Constraints(
                    minWidth = maxTitleWidth,
                    maxWidth = maxTitleWidth,
                    minHeight = currentTitleHeight,
                    maxHeight = currentTitleHeight,
                )
            )
        val subtitleConstraints =
            Constraints(
                minWidth = 0,
                maxWidth = maxTitleWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            )
        val subtitlePlaceable = measurables
            .firstOrNull { it.layoutId == "subtitle" }
            ?.measure(subtitleConstraints)

        val expandedTitleY =
            topInsetPx +
                bottomInsetPx +
                effectiveExpandedHeightPx -
                expandedGroupHeight -
                expandedBottomPadding

        val heightOffsetPx = scrollBehavior?.state?.heightOffset ?: 0f
        val currentContentHeight =
            (effectiveExpandedHeightPx + heightOffsetPx.roundToInt())
                .coerceAtLeast(collapsedHeightPx)
        val desiredHeight = topInsetPx + bottomInsetPx + currentContentHeight
        val layoutHeight = desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            val progress = collapseFraction()
            val subtitleScale = 1f + (collapsedSubtitleScale - 1f) * progress

            val titlePositionX =
                (expandedTitleX + (collapsedTitleX - expandedTitleX) * progress).roundToInt()

            val collapsedTitleHeight = titleLineHeightPx * collapsedTitleScale
            val visualSubtitleHeight = (subtitlePlaceable?.height ?: 0) * subtitleScale
            val collapsedVisualHeight = collapsedTitleHeight + visualSubtitleHeight
            val collapsedVisualTop =
                topInsetPx + (collapsedHeightPx - collapsedVisualHeight) / 2f
            val collapsedTitleY = collapsedVisualTop

            val titlePositionY =
                (expandedTitleY + (collapsedTitleY - expandedTitleY) * progress).roundToInt()
            val subtitlePositionY = titlePositionY + currentTitleHeight

            navigationPlaceable.placeRelative(
                x = startInsetPx,
                y = topInsetPx + (collapsedHeightPx - navigationPlaceable.height) / 2,
            )

            titlePlaceable.placeRelative(titlePositionX, titlePositionY)
            subtitlePlaceable?.placeRelativeWithLayer(
                x = titlePositionX,
                y = subtitlePositionY,
            ) {
                scaleX = subtitleScale
                scaleY = subtitleScale
                transformOrigin = TransformOrigin(0f, 0f)
            }

            actionIconsPlaceable.placeRelative(
                x = layoutWidth - endInsetPx - actionIconsPlaceable.width,
                y = topInsetPx + (collapsedHeightPx - actionIconsPlaceable.height) / 2,
            )
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleScaledTopAppBar(
    state: TopAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }

    var remainingVelocity = velocity
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = velocity).animateDecay(
            flingAnimationSpec
        ) {
            val delta = value - lastValue
            val initialHeightOffset = state.heightOffset
            state.heightOffset = initialHeightOffset + delta
            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }
    }

    if (snapAnimationSpec != null &&
        state.heightOffset < 0f &&
        state.heightOffset > state.heightOffsetLimit
    ) {
        AnimationState(initialValue = state.heightOffset).animateTo(
            targetValue =
                if (state.collapsedFraction < 0.5f) 0f else state.heightOffsetLimit,
            animationSpec = snapAnimationSpec,
        ) {
            state.heightOffset = value
        }
    }

    return Velocity(0f, remainingVelocity)
}
