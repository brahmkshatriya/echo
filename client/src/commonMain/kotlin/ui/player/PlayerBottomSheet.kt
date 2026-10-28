package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.playback.PlayerSession
import dev.brahmkshatriya.echo.app.playback.ProvidePlaybackSession
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import dev.brahmkshatriya.echo.app.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.app.ui.components.blurFadePagerTransition
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.player.artwork.MaxPlayerArtworkSize
import dev.brahmkshatriya.echo.app.ui.player.artwork.PlayerArtworkHost
import dev.brahmkshatriya.echo.app.ui.player.components.PlayerPillTransition
import dev.brahmkshatriya.echo.app.ui.player.components.rememberPlayerPillTransition
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<BetterSheet?> { null }
val LocalPlayerControls = staticCompositionLocalOf<PlayerSession> {
    error("PlayerBottomSheet must provide player controls")
}
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

val LocalPlayerPagerState =
    staticCompositionLocalOf<PagerState?> { null }
internal val LocalPlayerSkipNavigator =
    staticCompositionLocalOf<PlayerSkipNavigator?> { null }
internal val LocalPlayerQueueTransition =
    staticCompositionLocalOf<PlayerPillTransition> { error("Player navigation must provide the queue transition") }
@Composable
private fun ProvidePlayerNavigation(
    pagerState: PagerState,
    content: @Composable () -> Unit,
) {
    val controls = LocalPlayerControls.current
    val scope = rememberCoroutineScope()
    val skipNavigator = remember(pagerState, controls) {
        PlayerSkipNavigator(pagerState, controls, scope)
    }
    val queueTransition = rememberPlayerPillTransition("queue")
    CompositionLocalProvider(
        LocalPlayerPagerState provides pagerState,
        LocalPlayerSkipNavigator provides skipNavigator,
        LocalPlayerQueueTransition provides queueTransition,
        content = content,
    )
}


@Composable
fun Modifier.applyPlayerTranslation() = run {
    val playerSheet = LocalPlayerSheet.current ?: return@run this
    val thresholdPx = with(LocalDensity.current) { playerSheet.peekHeight.toPx() / 2f }
    offset {
        val y = playerSheet.offsetState.value - playerSheet.midPointState.intValue
        val translation = if (y <= thresholdPx) y else (2f * thresholdPx - y).coerceAtLeast(0f)
        IntOffset(0, translation.roundToInt())
    }
}

private fun Modifier.reserveSystemGestureEdges(
    leftInsetPx: Int,
    rightInsetPx: Int,
): Modifier = pointerInput(leftInsetPx, rightInsetPx) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val fromLeftEdge = leftInsetPx > 0 && down.position.x <= leftInsetPx
        val fromRightEdge = rightInsetPx > 0 && down.position.x >= size.width - rightInsetPx
        if (!fromLeftEdge && !fromRightEdge) return@awaitEachGesture

        val start = down.position
        var claimedForSystemBack = false

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            if (!claimedForSystemBack) {
                val delta = change.position - start
                val horizontalDrag = abs(delta.x) > viewConfiguration.touchSlop &&
                        abs(delta.x) > abs(delta.y)
                val movingInward =
                    (fromLeftEdge && delta.x > 0f) || (fromRightEdge && delta.x < 0f)

                if (horizontalDrag && movingInward) {
                    claimedForSystemBack = true
                } else if (
                    abs(delta.y) > viewConfiguration.touchSlop ||
                    (abs(delta.x) > viewConfiguration.touchSlop && !movingInward)
                ) break
            }

            if (claimedForSystemBack) {
                event.changes.forEach { it.consume() }
            }
        }
    }
}

@Composable
fun PlayerBottomSheet(
    controls: PlayerSession,
    betterSheet: BetterSheet,
    startPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val actualBottomPadding = safePadding.calculateBottomPadding() + bottomPadding

    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val startPadding = startPadding + safePadding.calculateStartPadding(layoutDirection)
    val endPadding = safePadding.calculateEndPadding(layoutDirection)
    val systemGestureInsets = WindowInsets.systemGestures
    val leftSystemGestureInset = systemGestureInsets.getLeft(density, layoutDirection)
    val rightSystemGestureInset = systemGestureInsets.getRight(density, layoutDirection)
    val snackbarAnchors = remember { dev.brahmkshatriya.echo.app.ui.errors.PlayerSnackbarAnchors() }
    val presentation = rememberPlayerPresentationState()
    CompositionLocalProvider(
        dev.brahmkshatriya.echo.app.ui.errors.LocalPlayerSnackbarAnchors provides snackbarAnchors,
        LocalPlayerPresentation provides presentation,
        LocalPlayerSheet provides betterSheet,
        LocalPlayerControls provides controls,
        LocalPlayerPadding provides remember(startPadding, endPadding, bottomPadding) {
            PaddingValues(start = startPadding, end = endPadding, bottom = bottomPadding)
        }
    ) {
        val pagerState = rememberPagerState(0, pageCount = { controls.playbackOrder.size })
        val currentEntryId = controls.currentEntryId
        val sheetState = betterSheet.sheetState
        val hasQueue = controls.hasQueue
        val queueGeneration = controls.queueGeneration
        // Queue mutations own visibility. Keep this animation at the player root
        // so navigation and replacing compact content cannot cancel it.
        LaunchedEffect(sheetState, controls, hasQueue, queueGeneration) {
            if (!hasQueue) {
                sheetState.hide()
            } else if (sheetState.targetValue == SheetValue.Hidden) {
                sheetState.show()
            }
        }
        LaunchedEffect(sheetState, controls, hasQueue, queueGeneration) {
            if (!hasQueue) return@LaunchedEffect
            // A restored queue can still be at the Hidden anchor while its
            // reveal starts. Only a subsequent dismissal should empty it.
            snapshotFlow { sheetState.targetValue != SheetValue.Hidden }.first { it }
            snapshotFlow {
                sheetState.currentValue == SheetValue.Hidden &&
                    sheetState.targetValue == SheetValue.Hidden
            }.first { it }
            if (controls.queueGeneration == queueGeneration &&
                sheetState.currentValue == SheetValue.Hidden &&
                sheetState.targetValue == SheetValue.Hidden) {
                controls.clearQueue()
            }
        }
        val pageScrolledToTop = remember { mutableStateMapOf<Int, Boolean>() }
        val pageInteractionLocked = remember { mutableStateMapOf<Int, Boolean>() }
        LaunchedEffect(controls.queueGeneration) {
            snackbarAnchors.clear()
            pageScrolledToTop.clear()
            pageInteractionLocked.clear()
        }
        val interactionLocked by remember {
            derivedStateOf {
                pageInteractionLocked[controls.entryIdAt(pagerState.currentPage)] == true
            }
        }
        val backGestureActive by remember {
            derivedStateOf { betterSheet.backProgressState.floatValue > 0f }
        }
        val pagerUserScrollEnabled by remember {
            derivedStateOf {
                !interactionLocked &&
                        !backGestureActive &&
                        pageScrolledToTop[controls.entryIdAt(pagerState.currentPage)] != false
            }
        }

        LaunchedEffect(pagerState, betterSheet) {
            snapshotFlow { betterSheet.backProgressState.floatValue > 0f }
                .distinctUntilChanged()
                .filter { it }
                .collectLatest {
                    if (pagerState.isScrollInProgress || pagerState.currentPageOffsetFraction != 0f) {
                        pagerState.scrollToPage(pagerState.settledPage)
                    }
                }
        }

        val modifier = Modifier.graphicsLayer {
            val backProgress = betterSheet.backProgressState.floatValue
            val scale = 1 - 0.15f * backProgress
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        }
        ProvidePlaybackSession(controls, pagerState) {
        Box(Modifier.fillMaxSize()) {
        BetterSheetScaffold(
            sheetContent = {
                BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
                    val sheetProgress by betterSheet.progressState
                    alpha = 1 + sheetProgress.coerceIn(-1f, 0f)
                    val backProgress = betterSheet.backProgressState.floatValue
                    val scale = 1f - 0.15f * backProgress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }) {
                    val queueViewport =
                        Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                    if (controls.hasQueue) {
                        ProvidePlayerNavigation(pagerState) {
                            val queueTransition = LocalPlayerQueueTransition.current
                            val skipNavigator = LocalPlayerSkipNavigator.current
                            ProvidePlayerPresentation {
                                Box(Modifier.fillMaxSize()) {
                                    HorizontalPager(
                                        pagerState,
                                        Modifier
                                            .fillMaxSize()
                                            .reserveSystemGestureEdges(
                                                leftInsetPx = leftSystemGestureInset,
                                                rightInsetPx = rightSystemGestureInset,
                                            ),
                                        userScrollEnabled = pagerUserScrollEnabled,
                                        key = { controls.entryIdAt(it) },
                                    ) { page ->
                                        val entryId = controls.entryIdAt(page)
                                        val effectProgress = {
                                            if (betterSheet.backProgressState.floatValue > 0f) 0f
                                            else betterSheet.progressState.value.coerceIn(0f, 1f)
                                        }
                                        Box(
                                            Modifier.fillMaxSize()
                                                .blurFadePagerTransition(pagerState, page, effectProgress)
                                                .animatedSkipEntrance(skipNavigator, entryId, effectProgress)
                                        ) {
                                            MediaPlayerPage(
                                                i = entryId,
                                                onScrolledToTopChanged = { scrolledToTop ->
                                                    pageScrolledToTop[entryId] = scrolledToTop
                                                },
                                                onInteractionLockChanged = { locked ->
                                                    pageInteractionLocked[entryId] = locked
                                                },
                                            )
                                        }
                                    }
                                    if (queueTransition.open || queueTransition.mounted) {
                                        PlayerArtworkHost(
                                            index = currentEntryId,
                                            artworkMaxSize = MaxPlayerArtworkSize.dp,
                                            coloredBackground = false,
                                        ) {
                                            SharedPlayerQueueOverlay(
                                                transition = queueTransition,
                                                viewport = queueViewport,
                                                currentEntryId = currentEntryId,
                                                onEntrySelected = { entryId ->
                                                    skipNavigator?.selectEntry(entryId)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomPadding = actualBottomPadding,
            betterSheet = betterSheet,
            sheetShape = RectangleShape,
            sheetDragHandle = null,
            snackBarHost = {},
            sheetShadowElevation = 0.dp,
            sheetContainerColor = Color.Unspecified,
            sheetMaxWidth = Dp.Unspecified,
            sheetSwipeEnabled = controls.hasQueue && !interactionLocked,
            containerColor = Color.Unspecified,
            content = {
                Box(Modifier.paddingMask().then(modifier).applyPlayerTranslation()) {
                    content()
                }
            }
        )
        dev.brahmkshatriya.echo.app.ui.errors.PlaybackErrors(
            controls, betterSheet, snackbarAnchors, startPadding, endPadding, bottomPadding,
        )
        }
        }
    }
}
