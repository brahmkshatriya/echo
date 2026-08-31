package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A seek bar whose active track can be rendered as a configurable squiggle.
 *
 * [segmentGaps] contains values in [valueRange] where the visual track should be split. Seeking
 * remains continuous across a gap; the gaps are purely visual. This makes them suitable for things
 * such as chapter or media-segment boundaries. [waveSpeed] follows Material's wavy progress
 * indicator convention: by default the wave travels one [squiggleWavelength] per second.
 */
@Composable
fun SquigglySeekBar(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    segmentGaps: List<Float> = emptyList(),
    segmentGapWidth: Dp = 2.dp,
    squiggleAmplitude: Float = 0.66f,
    squiggleWavelength: Dp = 40.dp,
    waveSpeed: Dp = squiggleWavelength,
    squigglePhase: Float = 0f,
    trackHeight: Dp = 16.dp,
    trackStrokeWidth: Dp = 4.dp,
    draggedTrackStrokeWidth: Dp = 8.dp,
    trackCornerSize: Dp = Dp.Unspecified,
    trackInsideCornerSize: Dp = 2.dp,
    stopIndicatorSize: Dp = 4.dp,
    thumbSize: DpSize = DpSize(4.dp, 32.dp),
    thumbTrackGap: Dp = 4.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    stopIndicatorColor: Color = activeColor,
    thumbColor: Color = activeColor,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished = rememberUpdatedState(onValueChangeFinished)
    val currentValueRange = rememberUpdatedState(valueRange)
    val layoutDirection = LocalLayoutDirection.current
    val waveOffset = remember { Animatable(0f) }
    val isDragged by source.collectIsDraggedAsState()
    val animatedThumbWidth by animateDpAsState(
        targetValue = if (isDragged) thumbSize.width / 2f else thumbSize.width,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "Squiggly seek bar thumb width",
    )
    val animatedTrackStrokeWidth by animateDpAsState(
        targetValue = if (isDragged) draggedTrackStrokeWidth else trackStrokeWidth,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "Squiggly seek bar track width",
    )
    val animatedSquiggleAmplitude by animateFloatAsState(
        targetValue = squiggleAmplitude.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "Squiggly seek bar amplitude",
    )

    LaunchedEffect(squiggleAmplitude, squiggleWavelength, waveSpeed) {
        if (squiggleAmplitude <= 0f || squiggleWavelength <= 0.dp || waveSpeed <= 0.dp) {
            waveOffset.snapTo(0f)
            return@LaunchedEffect
        }

        val durationMillis = ((squiggleWavelength / waveSpeed) * 1_000f)
            .roundToInt()
            .coerceAtLeast(1)
        while (true) {
            val start = waveOffset.value % 1f
            waveOffset.snapTo(start)
            waveOffset.animateTo(
                targetValue = start + 1f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
        }
    }

    val segmentGapFractions = remember(segmentGaps, valueRange) {
        segmentGaps.map { valueFraction(it, valueRange) }
    }

    fun valueAt(x: Float, width: Float, thumbWidth: Float): Float {
        val trackStart = thumbWidth / 2f
        val trackLength = width - thumbWidth
        if (trackLength <= 0f) return valueRange.start
        var fraction = ((x - trackStart) / trackLength).coerceIn(0f, 1f)
        if (layoutDirection == LayoutDirection.Rtl) fraction = 1f - fraction
        return valueForFraction(fraction, currentValueRange.value)
    }

    val inputModifier = if (!enabled) Modifier else Modifier
        .pointerInput(source, layoutDirection) {
            detectTapGestures(
                onTap = { offset ->
                    currentOnValueChange.value(
                        valueAt(offset.x, size.width.toFloat(), thumbSize.width.toPx()),
                    )
                    currentOnValueChangeFinished.value?.invoke()
                },
            )
        }
        .pointerInput(source, layoutDirection) {
            var dragInteraction: DragInteraction.Start? = null

            fun finishDrag(cancelled: Boolean) {
                dragInteraction?.let { start ->
                    source.tryEmit(
                        if (cancelled) DragInteraction.Cancel(start)
                        else DragInteraction.Stop(start),
                    )
                }
                dragInteraction = null
                if (!cancelled) currentOnValueChangeFinished.value?.invoke()
            }

            detectDragGestures(
                onDragStart = { offset ->
                    val start = DragInteraction.Start()
                    dragInteraction = start
                    source.tryEmit(start)
                    currentOnValueChange.value(
                        valueAt(offset.x, size.width.toFloat(), thumbSize.width.toPx()),
                    )
                },
                onDragEnd = { finishDrag(cancelled = false) },
                onDragCancel = { finishDrag(cancelled = true) },
                onDrag = { change, _ ->
                    change.consume()
                    currentOnValueChange.value(
                        valueAt(change.position.x, size.width.toFloat(), thumbSize.width.toPx()),
                    )
                },
            )
        }

    Canvas(
        modifier = modifier
            .defaultMinSize(minHeight = thumbSize.height)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value().coerceIn(valueRange.start, valueRange.endInclusive),
                    range = valueRange,
                    steps = 0,
                )
                if (enabled) {
                    setProgress { targetValue ->
                        currentOnValueChange.value(
                            targetValue.coerceIn(valueRange.start, valueRange.endInclusive),
                        )
                        currentOnValueChangeFinished.value?.invoke()
                        true
                    }
                }
            }
            .then(inputModifier),
    ) {
        val progress = valueFraction(
            value().coerceIn(valueRange.start, valueRange.endInclusive),
            valueRange,
        )
        val halfThumbTravelInset = thumbSize.width.toPx() / 2f
        val trackEnd = max(halfThumbTravelInset, size.width - halfThumbTravelInset)
        val trackLength = trackEnd - halfThumbTravelInset
        val strokeWidth = animatedTrackStrokeWidth.toPx().coerceAtLeast(0f)
        val maxCornerRadius = strokeWidth / 2f
        val outerCornerRadius = if (trackCornerSize == Dp.Unspecified) {
            maxCornerRadius
        } else {
            trackCornerSize.toPx().coerceIn(0f, maxCornerRadius)
        }
        val stopIndicatorRadius = stopIndicatorSize.toPx().coerceAtLeast(0f) / 2f
        val stopIndicatorCenter =
            (trackEnd - outerCornerRadius).coerceAtLeast(halfThumbTravelInset)
        val thumbCenter = halfThumbTravelInset + trackLength * progress
        val stopIndicatorVisibility = if (
            thumbCenter + animatedThumbWidth.toPx().coerceAtLeast(0f) / 2f +
            thumbTrackGap.toPx() < stopIndicatorCenter - stopIndicatorRadius
        ) 1f else 0f

        drawSeekBar(
            progress = progress,
            segmentGaps = segmentGapFractions,
            segmentGapWidthPx = segmentGapWidth.toPx(),
            squiggleAmplitude = animatedSquiggleAmplitude,
            squiggleWavelengthPx = squiggleWavelength.toPx(),
            squigglePhase = squigglePhase + waveOffset.value,
            trackHeightPx = trackHeight.toPx(),
            trackStrokeWidthPx = strokeWidth,
            trackCornerSize = trackCornerSize,
            trackInsideCornerSize = trackInsideCornerSize,
            stopIndicatorSizePx = stopIndicatorSize.toPx(),
            thumbTravelInsetWidthPx = thumbSize.width.toPx(),
            thumbWidthPx = animatedThumbWidth.toPx(),
            thumbHeightPx = thumbSize.height.toPx(),
            thumbTrackGapPx = thumbTrackGap.toPx(),
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            stopIndicatorColor = stopIndicatorColor,
            thumbColor = thumbColor,
            layoutDirection = layoutDirection,
            stopIndicatorVisibility = stopIndicatorVisibility,
        )
    }
}

private fun DrawScope.drawSeekBar(
    progress: Float,
    segmentGaps: List<Float>,
    segmentGapWidthPx: Float,
    squiggleAmplitude: Float,
    squiggleWavelengthPx: Float,
    squigglePhase: Float,
    trackHeightPx: Float,
    trackStrokeWidthPx: Float,
    trackCornerSize: Dp,
    trackInsideCornerSize: Dp,
    stopIndicatorSizePx: Float,
    thumbTravelInsetWidthPx: Float,
    thumbWidthPx: Float,
    thumbHeightPx: Float,
    thumbTrackGapPx: Float,
    activeColor: Color,
    inactiveColor: Color,
    stopIndicatorColor: Color,
    thumbColor: Color,
    layoutDirection: LayoutDirection,
    stopIndicatorVisibility: Float,
) {
    if (size.width <= 0f || size.height <= 0f) return

    val centerY = size.height / 2f
    val halfThumbTravelInset = thumbTravelInsetWidthPx / 2f
    val trackEnd = max(halfThumbTravelInset, size.width - halfThumbTravelInset)
    val trackLength = trackEnd - halfThumbTravelInset
    val strokeWidth = trackStrokeWidthPx.coerceAtLeast(0f)
    val gapWidth = segmentGapWidthPx.coerceAtLeast(0f)
    val physicalProgress = progress.coerceIn(0f, 1f)
    val maxCornerRadius = strokeWidth / 2f
    val outerCornerRadius = if (trackCornerSize == Dp.Unspecified) {
        maxCornerRadius
    } else {
        trackCornerSize.toPx().coerceIn(0f, maxCornerRadius)
    }
    val insideCornerRadius = trackInsideCornerSize.toPx().coerceIn(0f, maxCornerRadius)
    val thumbCenterLogicalX = halfThumbTravelInset + trackLength * physicalProgress
    val thumbHalfWidth = thumbWidthPx.coerceAtLeast(0f) / 2f
    val resolvedThumbTrackGap = thumbTrackGapPx.coerceAtLeast(0f)

    if (trackLength > 0f && strokeWidth > 0f) {
        val ranges = visibleTrackRanges(
            trackStart = halfThumbTravelInset,
            trackEnd = trackEnd,
            segmentGaps = segmentGaps,
            gapWidthPx = gapWidth,
        )

        val activeEnd = (
            thumbCenterLogicalX - thumbHalfWidth - resolvedThumbTrackGap
        ).coerceIn(halfThumbTravelInset, trackEnd)
        val inactiveStart = (
            thumbCenterLogicalX + thumbHalfWidth + resolvedThumbTrackGap
        ).coerceIn(halfThumbTravelInset, trackEnd)

        ranges.forEach { range ->
            val start = max(range.start, inactiveStart)
            val end = range.endInclusive
            if (end <= start) return@forEach
            drawStraightTrackSegment(
                startX = start,
                endX = end,
                trackStart = halfThumbTravelInset,
                trackEnd = trackEnd,
                centerY = centerY,
                strokeWidthPx = strokeWidth,
                startCornerRadiusPx = if (start <= halfThumbTravelInset) outerCornerRadius else insideCornerRadius,
                endCornerRadiusPx = if (end >= trackEnd) outerCornerRadius else insideCornerRadius,
                color = inactiveColor,
                layoutDirection = layoutDirection,
            )
        }

        val amplitudePx = (
            (trackHeightPx.coerceAtLeast(strokeWidth) - strokeWidth) / 2f *
                squiggleAmplitude.coerceIn(0f, 1f)
            ).coerceAtLeast(0f)
        val wavelengthPx = squiggleWavelengthPx.coerceAtLeast(1f)

        ranges.forEach { range ->
            val start = range.start
            val end = min(range.endInclusive, activeEnd)
            if (end <= start) return@forEach

            drawSquiggleTrackSegment(
                startX = start,
                endX = end,
                trackStart = halfThumbTravelInset,
                trackEnd = trackEnd,
                centerY = centerY,
                amplitudePx = amplitudePx,
                wavelengthPx = wavelengthPx,
                phase = squigglePhase,
                strokeWidthPx = strokeWidth,
                startCornerRadiusPx = if (start <= halfThumbTravelInset) outerCornerRadius else insideCornerRadius,
                endCornerRadiusPx = if (end >= trackEnd) outerCornerRadius else insideCornerRadius,
                color = activeColor,
                layoutDirection = layoutDirection,
            )
        }
    }

    val stopIndicatorRadius = stopIndicatorSizePx.coerceAtLeast(0f) / 2f
    val stopIndicatorCenterLogicalX =
        (trackEnd - outerCornerRadius).coerceAtLeast(halfThumbTravelInset)
    val stopIndicatorScale = stopIndicatorVisibility.coerceIn(0f, 1f)
    if (stopIndicatorRadius > 0f && stopIndicatorScale > 0f) {
        drawCircle(
            color = stopIndicatorColor.copy(alpha = stopIndicatorColor.alpha * stopIndicatorScale),
            radius = stopIndicatorRadius * stopIndicatorScale,
            center = Offset(
                x = logicalToPhysicalX(
                    stopIndicatorCenterLogicalX,
                    halfThumbTravelInset,
                    trackEnd,
                    layoutDirection,
                ),
                y = centerY,
            ),
        )
    }

    val thumbCenterX = logicalToPhysicalX(
        thumbCenterLogicalX,
        halfThumbTravelInset,
        trackEnd,
        layoutDirection,
    )
    val resolvedThumbWidth = thumbWidthPx.coerceAtLeast(0f)
    val resolvedThumbHeight = min(thumbHeightPx.coerceAtLeast(0f), size.height)
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(
            x = thumbCenterX - resolvedThumbWidth / 2f,
            y = centerY - resolvedThumbHeight / 2f,
        ),
        size = Size(resolvedThumbWidth, resolvedThumbHeight),
        cornerRadius = CornerRadius(resolvedThumbWidth / 2f),
    )
}

private fun DrawScope.drawSquiggleTrackSegment(
    startX: Float,
    endX: Float,
    trackStart: Float,
    trackEnd: Float,
    centerY: Float,
    amplitudePx: Float,
    wavelengthPx: Float,
    phase: Float,
    strokeWidthPx: Float,
    startCornerRadiusPx: Float,
    endCornerRadiusPx: Float,
    color: Color,
    layoutDirection: LayoutDirection,
) {
    if (endX <= startX || strokeWidthPx <= 0f) return

    if (amplitudePx <= 0.01f) {
        drawStraightTrackSegment(
            startX = startX,
            endX = endX,
            trackStart = trackStart,
            trackEnd = trackEnd,
            centerY = centerY,
            strokeWidthPx = strokeWidthPx,
            startCornerRadiusPx = startCornerRadiusPx,
            endCornerRadiusPx = endCornerRadiusPx,
            color = color,
            layoutDirection = layoutDirection,
        )
        return
    }

    val halfThickness = strokeWidthPx / 2f
    val segmentLength = endX - startX
    val maxCornerRadius = min(halfThickness, segmentLength / 2f)
    val startRadius = startCornerRadiusPx.coerceIn(0f, maxCornerRadius)
    val endRadius = endCornerRadiusPx.coerceIn(0f, maxCornerRadius)
    val bodyStartX = startX + startRadius
    val bodyEndX = endX - endRadius
    val step = (wavelengthPx / 16f).coerceIn(0.5f, 2f)
    val circleKappa = 0.5522848f
    val angularFrequency = 2f * PI.toFloat() / wavelengthPx

    fun angleAt(x: Float): Float =
        ((x - trackStart) / wavelengthPx + phase) * 2f * PI.toFloat()

    fun waveY(x: Float): Float = centerY + sin(angleAt(x)) * amplitudePx

    fun physicalX(x: Float): Float =
        logicalToPhysicalX(x, trackStart, trackEnd, layoutDirection)

    fun edgePoint(x: Float, top: Boolean): Offset {
        val slope = cos(angleAt(x)) * amplitudePx * angularFrequency
        val normalLength = sqrt(1f + slope * slope)
        val normalX = slope / normalLength
        val normalY = 1f / normalLength
        val direction = if (top) -1f else 1f
        val logicalX = x + direction * -normalX * halfThickness
        val y = waveY(x) + direction * normalY * halfThickness
        return Offset(physicalX(logicalX), y)
    }

    val startCenterY = waveY(bodyStartX)
    val endCenterY = waveY(bodyEndX)
    val startTop = edgePoint(bodyStartX, top = true)
    val startBottom = edgePoint(bodyStartX, top = false)
    val endTop = edgePoint(bodyEndX, top = true)
    val endBottom = edgePoint(bodyEndX, top = false)
    val startBoundaryX = physicalX(startX)
    val endBoundaryX = physicalX(endX)
    val path = Path()

    path.moveTo(startTop.x, startTop.y)
    var x = bodyStartX + step
    while (x < bodyEndX) {
        val point = edgePoint(x, top = true)
        path.lineTo(point.x, point.y)
        x += step
    }
    path.lineTo(endTop.x, endTop.y)

    if (endRadius > 0f) {
        val endUpperY = endCenterY - halfThickness + endRadius
        path.cubicTo(
            endTop.x + (endBoundaryX - endTop.x) * circleKappa,
            endTop.y,
            endBoundaryX,
            endUpperY - circleKappa * endRadius,
            endBoundaryX,
            endUpperY,
        )
    } else {
        path.lineTo(endBoundaryX, endCenterY - halfThickness)
    }
    path.lineTo(endBoundaryX, endCenterY + halfThickness - endRadius)
    if (endRadius > 0f) {
        val endLowerY = endCenterY + halfThickness - endRadius
        path.cubicTo(
            endBoundaryX,
            endLowerY + circleKappa * endRadius,
            endBottom.x + (endBoundaryX - endBottom.x) * circleKappa,
            endBottom.y,
            endBottom.x,
            endBottom.y,
        )
    } else {
        path.lineTo(endBottom.x, endBottom.y)
    }

    x = bodyEndX - step
    while (x > bodyStartX) {
        val point = edgePoint(x, top = false)
        path.lineTo(point.x, point.y)
        x -= step
    }
    path.lineTo(startBottom.x, startBottom.y)

    if (startRadius > 0f) {
        val startLowerY = startCenterY + halfThickness - startRadius
        path.cubicTo(
            startBottom.x + (startBoundaryX - startBottom.x) * circleKappa,
            startBottom.y,
            startBoundaryX,
            startLowerY + circleKappa * startRadius,
            startBoundaryX,
            startLowerY,
        )
    } else {
        path.lineTo(startBoundaryX, startCenterY + halfThickness)
    }
    path.lineTo(startBoundaryX, startCenterY - halfThickness + startRadius)
    if (startRadius > 0f) {
        val startUpperY = startCenterY - halfThickness + startRadius
        path.cubicTo(
            startBoundaryX,
            startUpperY - circleKappa * startRadius,
            startTop.x + (startBoundaryX - startTop.x) * circleKappa,
            startTop.y,
            startTop.x,
            startTop.y,
        )
    } else {
        path.lineTo(startTop.x, startTop.y)
    }
    path.close()

    drawPath(path = path, color = color)
}

private fun DrawScope.drawStraightTrackSegment(
    startX: Float,
    endX: Float,
    trackStart: Float,
    trackEnd: Float,
    centerY: Float,
    strokeWidthPx: Float,
    startCornerRadiusPx: Float,
    endCornerRadiusPx: Float,
    color: Color,
    layoutDirection: LayoutDirection,
) {
    if (endX <= startX || strokeWidthPx <= 0f) return

    val physicalStart = logicalToPhysicalX(startX, trackStart, trackEnd, layoutDirection)
    val physicalEnd = logicalToPhysicalX(endX, trackStart, trackEnd, layoutDirection)
    val left = min(physicalStart, physicalEnd)
    val right = max(physicalStart, physicalEnd)
    val halfHeight = strokeWidthPx / 2f
    val maxRadius = min(halfHeight, (right - left) / 2f)
    val logicalStartRadius = startCornerRadiusPx.coerceIn(0f, maxRadius)
    val logicalEndRadius = endCornerRadiusPx.coerceIn(0f, maxRadius)
    val leftRadius = if (layoutDirection == LayoutDirection.Ltr) {
        logicalStartRadius
    } else {
        logicalEndRadius
    }
    val rightRadius = if (layoutDirection == LayoutDirection.Ltr) {
        logicalEndRadius
    } else {
        logicalStartRadius
    }
    val leftCorner = CornerRadius(leftRadius, leftRadius)
    val rightCorner = CornerRadius(rightRadius, rightRadius)
    val trackPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(left, centerY - halfHeight, right, centerY + halfHeight),
                topLeft = leftCorner,
                bottomLeft = leftCorner,
                topRight = rightCorner,
                bottomRight = rightCorner,
            ),
        )
    }
    drawPath(trackPath, color)
}

private fun visibleTrackRanges(
    trackStart: Float,
    trackEnd: Float,
    segmentGaps: List<Float>,
    gapWidthPx: Float,
): List<ClosedFloatingPointRange<Float>> {
    if (trackEnd <= trackStart) return emptyList()

    val trackLength = trackEnd - trackStart
    val halfCut = gapWidthPx / 2f
    val gaps = segmentGaps
        .asSequence()
        .filter { it > 0f && it < 1f }
        .map { trackStart + trackLength * it }
        .sorted()
        .toList()

    if (gaps.isEmpty()) return listOf(trackStart..trackEnd)

    val ranges = mutableListOf<ClosedFloatingPointRange<Float>>()
    var rangeStart = trackStart
    gaps.forEach { gapCenter ->
        val rangeEnd = (gapCenter - halfCut).coerceIn(trackStart, trackEnd)
        if (rangeEnd > rangeStart) ranges += rangeStart..rangeEnd
        rangeStart = max(rangeStart, (gapCenter + halfCut).coerceIn(trackStart, trackEnd))
    }
    if (trackEnd > rangeStart) ranges += rangeStart..trackEnd
    return ranges
}

private fun logicalToPhysicalX(
    x: Float,
    trackStart: Float,
    trackEnd: Float,
    layoutDirection: LayoutDirection,
): Float = when (layoutDirection) {
    LayoutDirection.Ltr -> x
    LayoutDirection.Rtl -> trackEnd - (x - trackStart)
}

private fun valueFraction(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val length = valueRange.endInclusive - valueRange.start
    if (length <= 0f) return 0f
    return ((value - valueRange.start) / length).coerceIn(0f, 1f)
}

private fun valueForFraction(
    fraction: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float = valueRange.start +
    (valueRange.endInclusive - valueRange.start) * fraction.coerceIn(0f, 1f)
