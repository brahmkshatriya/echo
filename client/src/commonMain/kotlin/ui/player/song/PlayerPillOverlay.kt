package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal class PlayerPillTransition(openState: MutableState<Boolean>) {
    var open by openState
        private set
    var mounted by mutableStateOf(openState.value)
        internal set
    var restoring by mutableStateOf(openState.value)
        internal set
    var source by mutableStateOf<Rect?>(null)
        private set
    // Rotation can settle bottom-bar placement over multiple layout passes.
    private var trackingRestoredAnchor = openState.value
    var backGestureActive by mutableStateOf(false)
        internal set

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

    fun updateRestoredAnchor(bounds: Rect) {
        if (trackingRestoredAnchor && source != bounds) source = bounds
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
internal fun rememberPlayerPillTransition(key: String): PlayerPillTransition {
    val savedOpen = rememberSaveable(key) { mutableStateOf(false) }
    return remember(key, savedOpen) { PlayerPillTransition(savedOpen) }
}

/** Reusable animation wrapper. Each caller provides its own real page and pill content. */
@Composable
internal fun PlayerPillOverlay(
    transition: PlayerPillTransition,
    viewport: Size,
    isCurrentPage: Boolean,
    pillColor: Color,
    openInfo: NavigationEventInfo,
    closedInfo: NavigationEventInfo,
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

    val navigationState = rememberNavigationEventState(
        currentInfo = if (transition.open) openInfo else closedInfo,
        backInfo = if (transition.open) listOf(closedInfo) else emptyList(),
    )
    LaunchedEffect(navigationState) {
        snapshotFlow {
            when (val state = navigationState.transitionState) {
                NavigationEventTransitionState.Idle -> null
                is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
            }
        }.collectLatest { progress ->
            if (progress != null && isCurrentPage) {
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

    LaunchedEffect(transition.open, transition.source, viewport) {
        val source = transition.source
        if (transition.open) {
            if (transition.restoring) {
                coroutineScope {
                    launch { transition.center.snapTo(Offset(viewport.width / 2f, viewport.height / 2f)) }
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
                    launch { transition.radius.snapTo(source.height / 2f) }
                    launch { transition.reveal.snapTo(0f) }
                    launch { transition.contentScale.snapTo(0.66f) }
                }
            }
            coroutineScope {
                launch { transition.center.animateTo(Offset(viewport.width / 2f, viewport.height / 2f), spatial) }
                launch { transition.size.animateTo(viewport, sizeSpec) }
                launch { transition.radius.animateTo(0f, radiusSpec) }
                launch { transition.reveal.animateTo(1f, fadeSpec) }
                launch { transition.contentScale.animateTo(1f, fadeSpec) }
            }
        } else if (transition.mounted) {
            val exitSource = source ?: Rect(
                viewport.width / 2f - 24f, viewport.height - 64f,
                viewport.width / 2f + 24f, viewport.height - 16f,
            )
            coroutineScope {
                launch { transition.center.animateTo(exitSource.center, spatial) }
                launch { transition.size.animateTo(exitSource.size, sizeSpec) }
                launch { transition.radius.animateTo(exitSource.height / 2f, radiusSpec) }
                launch { transition.reveal.animateTo(0f, fadeSpec) }
                launch { transition.contentScale.animateTo(0.66f, fadeSpec) }
            }
            transition.finishExit()
        }
    }

    if (!transition.mounted) return
    val progress = transition.reveal.value.coerceIn(0f, 1f)
    val back = if (transition.backGestureActive) {
        transition.backProgress.value.coerceIn(0f, 1f)
    } else 0f
    val center = transition.center.value
    val size = transition.size.value
    val left = center.x - size.width / 2f
    val top = center.y - size.height / 2f
    val pillAlpha = (1f - progress / 0.5f).coerceIn(0f, 1f)
    val pageAlpha = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val source = transition.source ?: Rect(left, top, left + size.width, top + size.height)

    Box(Modifier.fillMaxSize()
        .zIndex(if (transition.open) 2f else 1f)
        .graphicsLayer {
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
                .clip(RoundedCornerShape(with(density) { transition.radius.value.coerceAtLeast(0f).toDp() }))
                .background(lerp(pillColor, colorScheme.surfaceContainer, progress))
                .clickable(interactionSource = null, indication = null) {
                    if (transition.open) transition.close() else transition.openFrom(source)
                },
        )
        if (pillAlpha > 0f) {
            Box(Modifier.size(
                with(density) { source.width.coerceAtLeast(1f).toDp() },
                with(density) { source.height.coerceAtLeast(1f).toDp() },
            ).graphicsLayer {
                translationX = center.x - source.width / 2f
                translationY = center.y - source.height / 2f
                alpha = pillAlpha
            }) { pillContent() }
        }
        if (pageAlpha > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = pageAlpha
                scaleX = transition.contentScale.value
                scaleY = transition.contentScale.value
                translationX = center.x - viewport.width / 2f
                translationY = center.y - viewport.height / 2f
            }) { pageContent(transition.open) }
        }
        if (!transition.open || progress < 0.5f) {
            Box(
                Modifier.offset { IntOffset(source.left.roundToInt(), source.top.roundToInt()) }
                    .size(
                        with(density) { source.width.coerceAtLeast(1f).toDp() },
                        with(density) { source.height.coerceAtLeast(1f).toDp() },
                    )
                    .clickable(interactionSource = null, indication = null) {
                        if (transition.open) transition.close() else transition.openFrom(source)
                    },
            )
        }
        if (!transition.open && alternateSource != null) {
            Box(
                Modifier.offset {
                    IntOffset(alternateSource.left.roundToInt(), alternateSource.top.roundToInt())
                }.size(
                    with(density) { alternateSource.width.coerceAtLeast(1f).toDp() },
                    with(density) { alternateSource.height.coerceAtLeast(1f).toDp() },
                ).clickable(interactionSource = null, indication = null, onClick = onAlternateClick),
            )
        }
    }
}
