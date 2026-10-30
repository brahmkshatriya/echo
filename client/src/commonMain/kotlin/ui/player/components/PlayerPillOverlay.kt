package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.graphics.shapes.RoundedPolygon
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerPillTransition(openState: MutableState<Boolean>) {
    var open by openState
        private set

    var mounted by mutableStateOf(openState.value)
    var restoring by mutableStateOf(openState.value)
    var source by mutableStateOf<Rect?>(null)
        private set

    var alternateSource by mutableStateOf<Rect?>(null)
    var onAlternateClick: (() -> Unit)? = null
    // Rotation can settle bottom-bar placement over multiple layout passes.
    private var trackingRestoredAnchor = openState.value
    var backGestureActive by mutableStateOf(false)

    val backProgress = Animatable(0f)
    val center = Animatable(Offset.Zero, Offset.VectorConverter)
    val size = Animatable(Size.Zero, Size.VectorConverter)
    val radius = Animatable(0f)
    val reveal = Animatable(if (openState.value) 1f else 0f)
    val contentScale = Animatable(if (openState.value) 1f else 0.66f)

    fun openFrom(bounds: Rect) {
        trackingRestoredAnchor = false
        source = bounds
        open = true
    }

    fun updateAnchor(bounds: Rect) {
        if ((trackingRestoredAnchor || open || mounted) && source != bounds) source = bounds
    }

    fun close() {
        trackingRestoredAnchor = false
        open = false
    }

    fun finishExit() {
        mounted = false
        source = null
    }
}

@Composable
fun rememberPlayerPillTransition(key: String): PlayerPillTransition {
    val savedOpen = rememberSaveable(key) { mutableStateOf(false) }
    return remember(key, savedOpen) { PlayerPillTransition(savedOpen) }
}

/** Reusable animation wrapper. Each caller provides its own real page and pill content. */
@Composable
fun PlayerPillOverlay(
    transition: PlayerPillTransition,
    viewport: Size,
    isCurrentPage: Boolean,
    pillColor: Color,
    collapsedCornerRadius: Dp? = null,
    collapsedPolygon: RoundedPolygon? = null,
    openInfo: NavigationEventInfo,
    closedInfo: NavigationEventInfo,
    currentSource: (() -> Rect?)? = null,
    alternateSource: Rect? = null,
    onAlternateClick: () -> Unit = {},
    pillContent: @Composable () -> Unit,
    pageContent: @Composable (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val spatial = motionScheme.defaultSpatialSpec<Offset>()
    val sizeSpec = motionScheme.defaultEffectsSpec<Size>()
    val radiusSpec = motionScheme.defaultSpatialSpec<Float>()
    val fadeSpec = motionScheme.defaultSpatialSpec<Float>()
    val scope = rememberCoroutineScope()
    val collapsedRadiusPx = collapsedCornerRadius?.let { with(density) { it.toPx() } }
    val collapsedShape = collapsedPolygon?.toShape()
    // Remember the viewport only while this overlay is mounted. Reopening after
    // a window resize should still animate from the pill, not snap open.
    var previousViewport by remember(transition) { mutableStateOf(viewport) }

    val navigationState =
        rememberNavigationEventState(
            currentInfo = if (transition.open) openInfo else closedInfo,
            backInfo = if (transition.open) listOf(closedInfo) else emptyList(),
        )
    val currentIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val currentAnchorSource by rememberUpdatedState(currentSource)
    LaunchedEffect(navigationState, transition) {
        snapshotFlow {
                when (val state = navigationState.transitionState) {
                    NavigationEventTransitionState.Idle -> null
                    is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
                }
            }
            .collectLatest { progress ->
                if (progress != null && currentIsCurrentPage) {
                    transition.backGestureActive = true
                    transition.backProgress.snapTo(progress.coerceIn(0f, 1f))
                }
            }
    }
    if (LocalNavigationEventDispatcherOwner.current != null) {
        NavigationBackHandler(
            state = navigationState,
            isBackEnabled = isCurrentPage && transition.open,
            onBackCompleted = {
                transition.close()
                if (transition.backGestureActive) {
                    scope.launch {
                        transition.backProgress.animateTo(0f, fadeSpec)
                        transition.backGestureActive = false
                    }
                }
            },
            onBackCancelled = {
                scope.launch {
                    transition.backProgress.animateTo(0f, fadeSpec)
                    transition.backGestureActive = false
                }
            },
        )
    }

    val viewportChanged = previousViewport != viewport
    LaunchedEffect(
        transition,
        transition.open,
        viewport,
        collapsedRadiusPx,
        spatial,
        sizeSpec,
        radiusSpec,
        fadeSpec,
    ) {
        if (viewportChanged && transition.open && transition.mounted) {
            coroutineScope {
                launch {
                    transition.center.snapTo(Offset(viewport.width / 2f, viewport.height / 2f))
                }
                launch { transition.size.snapTo(viewport) }
                launch { transition.radius.snapTo(0f) }
                launch { transition.reveal.snapTo(1f) }
                launch { transition.contentScale.snapTo(1f) }
            }
            previousViewport = viewport
            transition.restoring = false
            return@LaunchedEffect
        }
        previousViewport = viewport
        val source =
            if (!transition.open) {
                (currentAnchorSource?.invoke() ?: transition.source).also { bounds ->
                    if (bounds != null) transition.updateAnchor(bounds)
                }
            } else transition.source
        if (transition.open) {
            if (transition.restoring) {
                coroutineScope {
                    launch {
                        transition.center.snapTo(Offset(viewport.width / 2f, viewport.height / 2f))
                    }
                    launch { transition.size.snapTo(viewport) }
                    launch { transition.radius.snapTo(0f) }
                    launch { transition.reveal.snapTo(1f) }
                    launch { transition.contentScale.snapTo(1f) }
                }
                transition.mounted = true
                transition.restoring = false
            } else if (!transition.mounted) {
                if (source == null) return@LaunchedEffect
                transition.mounted = true
                coroutineScope {
                    launch { transition.center.snapTo(source.center) }
                    launch { transition.size.snapTo(source.size) }
                    launch { transition.radius.snapTo(collapsedRadiusPx ?: (source.height / 2f)) }
                    launch { transition.reveal.snapTo(0f) }
                    launch { transition.contentScale.snapTo(0.66f) }
                }
            }
            coroutineScope {
                launch {
                    transition.center.animateTo(
                        Offset(viewport.width / 2f, viewport.height / 2f),
                        spatial,
                    )
                }
                launch { transition.size.animateTo(viewport, sizeSpec) }
                launch { transition.radius.animateTo(0f, radiusSpec) }
                launch { transition.reveal.animateTo(1f, fadeSpec) }
                launch { transition.contentScale.animateTo(1f, fadeSpec) }
            }
        } else if (transition.mounted) {
            val exitSource =
                source
                    ?: Rect(
                        viewport.width / 2f - 24f,
                        viewport.height - 64f,
                        viewport.width / 2f + 24f,
                        viewport.height - 16f,
                    )
            coroutineScope {
                launch { transition.center.animateTo(exitSource.center, spatial) }
                launch { transition.size.animateTo(exitSource.size, sizeSpec) }
                launch {
                    transition.radius.animateTo(
                        collapsedRadiusPx ?: (exitSource.height / 2f),
                        radiusSpec,
                    )
                }
                launch { transition.reveal.animateTo(0f, fadeSpec) }
                launch { transition.contentScale.animateTo(0.66f, fadeSpec) }
            }
            transition.finishExit()
        }
    }

    if (!transition.mounted) return
    // LaunchedEffect runs after composition. Read the new viewport immediately
    // on a resize, rather than drawing one frame at the previous animation
    // bounds and waiting for its snapTo calls.
    val resizeSnap = viewportChanged && transition.open
    val progress = if (resizeSnap) 1f else transition.reveal.value.coerceIn(0f, 1f)
    val back =
        if (transition.backGestureActive) {
            transition.backProgress.value.coerceIn(0f, 1f)
        } else 0f
    val center =
        if (resizeSnap) Offset(viewport.width / 2f, viewport.height / 2f)
        else transition.center.value
    val size = if (resizeSnap) viewport else transition.size.value
    val left = center.x - size.width / 2f
    val top = center.y - size.height / 2f
    val pillAlpha = (1f - progress / 0.5f).coerceIn(0f, 1f)
    val pageAlpha = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val source = transition.source ?: Rect(left, top, left + size.width, top + size.height)
    val shapeChangeEnd = 0.5f
    val silhouetteBlend = (progress / shapeChangeEnd).coerceIn(0f, 1f)
    val pageCornerProgress = ((progress - shapeChangeEnd) / (1f - shapeChangeEnd)).coerceIn(0f, 1f)
    val roundedShape =
        RoundedCornerShape(
            if (collapsedShape != null) {
                (collapsedCornerRadius ?: 12.dp) * (1f - pageCornerProgress)
            } else {
                with(density) { transition.radius.value.coerceAtLeast(0f).toDp() }
            }
        )
    val fadingSilhouettes = collapsedShape != null && progress < shapeChangeEnd
    val backgroundColor =
        if (collapsedShape != null) {
            lerp(pillColor, colorScheme.surfaceContainer, silhouetteBlend)
        } else {
            lerp(pillColor, colorScheme.surfaceContainer, progress)
        }

    Box(
        Modifier.fillMaxSize().zIndex(if (transition.open) 2f else 1f).graphicsLayer {
            val scale = 1f - 0.1f * back
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
            shape = RoundedCornerShape(28.dp * back)
            clip = back > 0f
        }
    ) {
        if (transition.backGestureActive && transition.open) {
            Box(Modifier.matchParentSize().background(colorScheme.surfaceContainer))
        }
        Box(
            Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(
                    with(density) { size.width.coerceAtLeast(1f).toDp() },
                    with(density) { size.height.coerceAtLeast(1f).toDp() },
                )
                .then(
                    if (fadingSilhouettes) Modifier
                    else Modifier.clip(roundedShape).background(backgroundColor)
                )
                .clickable(interactionSource = null, indication = null) {
                    // Consume background taps without dismissing the expanded page.
                    if (!transition.open) transition.openFrom(source)
                }
        ) {
            if (collapsedShape != null && progress < shapeChangeEnd) {
                Box(
                    Modifier.align(Alignment.Center)
                        .size(
                            with(density) {
                                minOf(size.width, size.height).coerceAtLeast(1f).toDp()
                            }
                        )
                        .graphicsLayer { alpha = 1f - silhouetteBlend }
                        .clip(collapsedShape)
                        .background(backgroundColor)
                )
                Box(
                    Modifier.matchParentSize()
                        .graphicsLayer { alpha = silhouetteBlend }
                        .clip(roundedShape)
                        .background(backgroundColor)
                )
            }
            if (pillAlpha > 0f) {
                Box(
                    Modifier.align(Alignment.Center)
                        .requiredSize(
                            with(density) { source.width.coerceAtLeast(1f).toDp() },
                            with(density) { source.height.coerceAtLeast(1f).toDp() },
                        )
                        .graphicsLayer { alpha = pillAlpha }
                ) {
                    pillContent()
                }
            }
            if (pageAlpha > 0f) {
                Box(
                    Modifier.align(Alignment.Center)
                        .requiredSize(
                            with(density) { viewport.width.coerceAtLeast(1f).toDp() },
                            with(density) { viewport.height.coerceAtLeast(1f).toDp() },
                        )
                        .graphicsLayer {
                            alpha = pageAlpha
                            scaleX = if (resizeSnap) 1f else transition.contentScale.value
                            scaleY = if (resizeSnap) 1f else transition.contentScale.value
                        }
                ) {
                    pageContent(transition.open)
                }
            }
        }
        if (!transition.open || progress < 0.5f) {
            Box(
                Modifier.offset { IntOffset(source.left.roundToInt(), source.top.roundToInt()) }
                    .size(
                        with(density) { source.width.coerceAtLeast(1f).toDp() },
                        with(density) { source.height.coerceAtLeast(1f).toDp() },
                    )
                    .clickable(interactionSource = null, indication = null) {
                        if (!transition.open) transition.openFrom(source)
                    }
            )
        }
        if (!transition.open && alternateSource != null) {
            Box(
                Modifier.offset {
                        IntOffset(
                            alternateSource.left.roundToInt(),
                            alternateSource.top.roundToInt(),
                        )
                    }
                    .size(
                        with(density) { alternateSource.width.coerceAtLeast(1f).toDp() },
                        with(density) { alternateSource.height.coerceAtLeast(1f).toDp() },
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onAlternateClick,
                    )
            )
        }
    }
}
