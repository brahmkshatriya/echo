package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.brahmkshatriya.echo.app.ui.Media
import dev.brahmkshatriya.echo.app.ui.components.FastScrollbar
import dev.brahmkshatriya.echo.app.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.main.Header
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerLyricsChromeCollapsed
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerLyricsVisible
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPagerState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.PlayerBottomBarContentType
import dev.brahmkshatriya.echo.app.ui.player.PlayerScrollbarItemSizes
import dev.brahmkshatriya.echo.app.ui.player.QueuePage
import dev.brahmkshatriya.echo.app.ui.player.blockDescendantBringIntoView
import dev.brahmkshatriya.echo.app.ui.player.clipHeaderTop
import dev.brahmkshatriya.echo.app.ui.player.expandedListItemTransform
import dev.brahmkshatriya.echo.app.ui.player.playNext
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarHeight
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerScrollbarState
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerScrollbarThumbMover
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPage
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPillContent
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectorPageInfo
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectorPlayerInfo
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberLyricsSelections
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_queue_music
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private object PlayerHeroVisibleInfo : NavigationEventInfo()
private object PlayerHeroHiddenInfo : NavigationEventInfo()
private object QueuePlayerInfo : NavigationEventInfo()
private object QueueOverlayPageInfo : NavigationEventInfo()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongPlayerItem(
    i: Int,
    onScrolledToTopChanged: (Boolean) -> Unit = {},
    onLyricsSelectorOpenChanged: (Boolean) -> Unit = {},
) = CompositionLocalProvider(
    LocalContentColor provides colorScheme.onPrimaryContainer
) {
    val controls = LocalPlayerControls.current
    val pagerState = LocalPlayerPagerState.current
    val scope = rememberCoroutineScope()
    val isCurrentItem = pagerState == null || pagerState.currentPage == i
    val lyricsSelections = rememberLyricsSelections()
    val timelineState = rememberPlayerTimelineState(
        key = i,
        durationMs = 220_000f,
        running = isCurrentItem && controls?.isPlaying != false,
        onFinished = {
            if (controls?.repeatEnabled != true && pagerState != null) {
                scope.launch {
                    pagerState.playNext(controls?.shuffleEnabled == true)
                }
                true
            } else false
        },
    )
    // Two actual destinations, each with independent state and animations.
    // They only share the PlayerPillOverlay implementation.
    val lyricsTransition = rememberPlayerPillTransition("lyrics")
    val queueTransition = rememberPlayerPillTransition("queue")
    var playerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var lyricsPillCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var queueButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    CompositionLocalProvider(LocalPlayerTimelineState provides timelineState) {
        BoxWithConstraints {
            val playerSheet = LocalPlayerSheet.current
            val backStack = LocalMainBackStack.current
            val cardColors = CardDefaults.cardColors(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface
            )
            val listState = rememberLazyListState()
            val scrollbarItemSizes = remember { PlayerScrollbarItemSizes() }
            val bottomBarKey = remember(i) { "player-bottom-bar-$i" }
            val isBottomBarSticky by remember(bottomBarKey) {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val item =
                        layoutInfo.visibleItemsInfo.firstOrNull { it.key == bottomBarKey }
                    item != null && item.offset <= 0 && listState.firstVisibleItemIndex >= item.index
                }
            }
            var fallbackShowLyrics by rememberSaveable(i) { mutableStateOf(false) }
            val sharedLyricsVisible = LocalPlayerLyricsVisible.current
            val showLyrics = sharedLyricsVisible?.value ?: fallbackShowLyrics
            var fallbackLyricsChromeCollapsed by rememberSaveable(i) { mutableStateOf(false) }
            val sharedLyricsChromeCollapsed = LocalPlayerLyricsChromeCollapsed.current
            val lyricsChromeCollapsed =
                sharedLyricsChromeCollapsed?.value ?: fallbackLyricsChromeCollapsed
            val setLyricsChromeCollapsed: (Boolean) -> Unit = { collapsed ->
                if (isCurrentItem) {
                    if (sharedLyricsChromeCollapsed != null) {
                        sharedLyricsChromeCollapsed.value = collapsed
                    } else {
                        fallbackLyricsChromeCollapsed = collapsed
                    }
                }
            }
            var lyricsChromeInteractionNonce by remember(i) { mutableIntStateOf(0) }
            val onLyricsChromeInteraction: () -> Unit = {
                if (isCurrentItem && showLyrics) {
                    setLyricsChromeCollapsed(false)
                    lyricsChromeInteractionNonce++
                }
            }
            LaunchedEffect(showLyrics) {
                if (!showLyrics) lyricsChromeInteractionNonce = 0
            }
            LaunchedEffect(timelineState.isSeeking) {
                if (showLyrics && timelineState.isSeeking) {
                    onLyricsChromeInteraction()
                }
            }
            LaunchedEffect(
                showLyrics,
                lyricsChromeInteractionNonce,
                timelineState.isSeeking,
                isCurrentItem,
                isBottomBarSticky,
            ) {
                if (
                    !isCurrentItem ||
                    !showLyrics ||
                    timelineState.isSeeking ||
                    isBottomBarSticky
                ) return@LaunchedEffect

                delay(3_000.milliseconds)
                setLyricsChromeCollapsed(true)
            }
            // The current pager item owns both the morph and its two pill anchors.
            var selectedLyricsIndex by rememberSaveable(i) { mutableStateOf<Int?>(0) }
            val selectedLyrics = selectedLyricsIndex?.let(lyricsSelections::getOrNull)
            val lyricsMounted = lyricsTransition.open || lyricsTransition.mounted
            val queueMounted = queueTransition.open || queueTransition.mounted
            val selectorMounted = lyricsMounted || queueMounted
            val pillDensity = LocalDensity.current
            fun localPillBounds(anchor: LayoutCoordinates?): Rect? {
                val root = playerCoordinates
                if (root?.isAttached != true || anchor?.isAttached != true) return null
                return root.localBoundingBoxOf(anchor, clipBounds = false)
            }
            fun queuePillBounds(): Rect? {
                val bounds = localPillBounds(queueButtonCoordinates) ?: return null
                val container = IconButtonDefaults.smallContainerSize()
                val width = with(pillDensity) { container.width.toPx() }
                val height = with(pillDensity) { container.height.toPx() }
                return Rect(
                    bounds.center.x - width / 2f,
                    bounds.center.y - height / 2f,
                    bounds.center.x + width / 2f,
                    bounds.center.y + height / 2f,
                )
            }
            LaunchedEffect(playerCoordinates, lyricsPillCoordinates, queueButtonCoordinates, isBottomBarSticky) {
                if (!isBottomBarSticky) {
                    localPillBounds(lyricsPillCoordinates)?.let(lyricsTransition::updateRestoredAnchor)
                    queuePillBounds()?.let(queueTransition::updateRestoredAnchor)
                }
            }
            fun openLyrics(bounds: Rect) {
                queueTransition.close()
                lyricsTransition.openFrom(bounds)
            }
            fun openQueue(bounds: Rect) {
                lyricsTransition.close()
                queueTransition.openFrom(bounds)
            }
            LaunchedEffect(selectorMounted) {
                onLyricsSelectorOpenChanged(selectorMounted)
            }
            LaunchedEffect(showLyrics) {
                if (!showLyrics) {
                    lyricsTransition.close()
                }
            }
            val isPlayerScrolledToTop by remember {
                derivedStateOf { !listState.canScrollBackward }
            }
            val heroBackProgress = remember(i) { Animatable(0f) }
            val heroBackAnimationSpec = motionScheme.defaultSpatialSpec<Float>()
            var heroBackGestureActive by remember(i) { mutableStateOf(false) }
            var heroBackStartIndex by remember(i) { mutableIntStateOf(0) }
            var heroBackStartItemOffset by remember(i) { mutableIntStateOf(0) }
            var heroBackStartScrollOffset by remember(i) { mutableIntStateOf(0) }
            var heroBackFallbackSize by remember(i) { mutableIntStateOf(1) }
            var heroBackTotalItems by remember(i) { mutableIntStateOf(0) }
            val heroBackMaxPredictiveTravelPx = (constraints.maxHeight / 4).coerceAtLeast(1)

            fun captureHeroBackStart(): Boolean {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems <= 0 || !isBottomBarSticky || !listState.canScrollBackward) {
                    return false
                }

                layoutInfo.visibleItemsInfo.forEach { item ->
                    scrollbarItemSizes.update(item.index, item.size)
                }
                val viewportSize =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
                val fallbackSize = scrollbarItemSizes.fallbackSize(viewportSize)
                heroBackStartIndex = listState.firstVisibleItemIndex
                heroBackStartItemOffset = listState.firstVisibleItemScrollOffset
                heroBackStartScrollOffset = scrollbarItemSizes.estimatedScrollOffset(
                    firstVisibleIndex = heroBackStartIndex,
                    firstVisibleItemScrollOffset = heroBackStartItemOffset,
                    fallbackSize = fallbackSize,
                )
                heroBackFallbackSize = fallbackSize
                heroBackTotalItems = totalItems
                heroBackGestureActive = heroBackStartScrollOffset > 0
                return heroBackGestureActive
            }

            fun applyHeroBackProgress(progress: Float) {
                if (!heroBackGestureActive || heroBackTotalItems <= 0) return
                val predictiveTravel = minOf(
                    heroBackStartScrollOffset,
                    heroBackMaxPredictiveTravelPx,
                )
                val targetScrollOffset = (
                        heroBackStartScrollOffset -
                                predictiveTravel * progress.coerceIn(0f, 1f)
                        ).roundToInt().coerceAtLeast(0)
                val target = scrollbarItemSizes.itemAtOffset(
                    targetScrollOffset = targetScrollOffset,
                    totalItems = heroBackTotalItems,
                    fallbackSize = heroBackFallbackSize,
                )
                listState.requestScrollToItem(
                    index = (target ushr 32).toInt(),
                    scrollOffset = target.toInt(),
                )
            }

            val heroBackEnabled =
                isCurrentItem && !selectorMounted &&
                        (isBottomBarSticky || heroBackGestureActive)
            val heroBackNavigationEventState = rememberNavigationEventState(
                currentInfo = if (heroBackEnabled) PlayerHeroHiddenInfo else PlayerHeroVisibleInfo,
                backInfo = if (heroBackEnabled) listOf(PlayerHeroVisibleInfo) else emptyList(),
            )
            if (LocalNavigationEventDispatcherOwner.current != null) {
                NavigationBackHandler(
                    state = heroBackNavigationEventState,
                    isBackEnabled = heroBackEnabled,
                    onBackCompleted = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                            heroBackStartScrollOffset = 0
                            heroBackGestureActive = false
                            heroBackProgress.snapTo(0f)
                        }
                    },
                    onBackCancelled = {
                        scope.launch {
                            heroBackProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = heroBackAnimationSpec,
                            )
                            listState.scrollToItem(
                                heroBackStartIndex,
                                heroBackStartItemOffset,
                            )
                            heroBackGestureActive = false
                        }
                    },
                )
            }
            LaunchedEffect(heroBackNavigationEventState, heroBackProgress) {
                snapshotFlow {
                    when (val state = heroBackNavigationEventState.transitionState) {
                        NavigationEventTransitionState.Idle -> null
                        is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
                    }
                }.collectLatest { progress ->
                    if (progress != null) {
                        if (!heroBackGestureActive && !captureHeroBackStart()) {
                            return@collectLatest
                        }
                        heroBackProgress.snapTo(progress.coerceIn(0f, 1f))
                    }
                }
            }
            LaunchedEffect(heroBackProgress) {
                snapshotFlow { heroBackProgress.value }.collectLatest { progress ->
                    applyHeroBackProgress(progress)
                }
            }
            val layoutDirection = LocalLayoutDirection.current
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
            val topPadding = safeDrawing.calculateTopPadding()
            val bottomPadding = safeDrawing.calculateBottomPadding()
            val currentOnScrolledToTopChanged by rememberUpdatedState(onScrolledToTopChanged)

            LaunchedEffect(listState) {
                snapshotFlow { !listState.canScrollBackward }
                    .collect { currentOnScrolledToTopChanged(it) }
            }
            playerSheet?.let { sheet ->
                LaunchedEffect(sheet, listState) {
                    snapshotFlow { sheet.progressState.floatValue < 0.75f }.collect { shouldReset ->
                        if (shouldReset && listState.canScrollBackward) {
                            withFrameNanos { }
                            listState.scrollToItem(0)
                        }
                    }
                }
            }

            val playerViewportHeightPx = constraints.maxHeight
            val heroHeight = (maxHeight - topPadding - bottomPadding - playerBottomBarHeight)
                .coerceAtLeast(0.dp)
            val heroHeightPx = with(LocalDensity.current) { heroHeight.toPx().coerceAtLeast(1f) }
            val heroAlpha by remember(listState, heroHeightPx) {
                derivedStateOf {
                    val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
                        listState.firstVisibleItemScrollOffset.toFloat()
                    } else {
                        heroHeightPx
                    }
                    1f - (scrollOffset / heroHeightPx).coerceIn(0f, 1f)
                }
            }
            val coverViewportWidth = maxWidth
            Box(Modifier.fillMaxSize().graphicsLayer {
                val reveal = maxOf(lyricsTransition.reveal.value, queueTransition.reveal.value)
                val back = when {
                    lyricsTransition.backGestureActive -> lyricsTransition.backProgress.value.coerceIn(0f, 1f)
                    queueTransition.backGestureActive -> queueTransition.backProgress.value.coerceIn(0f, 1f)
                    else -> 0f
                }
                val pageProgress = (reveal / 0.45f).coerceIn(0f, 1f)
                val scale = 1f - 0.04f * pageProgress * (1f - back)
                scaleX = scale
                scaleY = scale
            }.onGloballyPositioned {
                playerCoordinates = it
            }) {
                val transformModifier = Modifier.expandedListItemTransform(playerSheet) {
                    playerViewportHeightPx
                }

                fun heroSnapTarget(directionY: Float): Int? {
                    val hero = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                        it.key == "player-hero-$i"
                    } ?: return null
                    val offset = listState.firstVisibleItemScrollOffset
                    if (listState.firstVisibleItemIndex != hero.index ||
                        offset <= 0 || hero.size <= 0
                    ) return null

                    return when {
                        directionY < 0f -> 1
                        directionY > 0f -> 0
                        offset >= hero.size / 8 -> 1
                        else -> 0
                    }
                }

                val heroSnapConnection = remember(listState, i) {
                    object : NestedScrollConnection {
                        override suspend fun onPreFling(available: Velocity): Velocity {
                            val target = heroSnapTarget(available.y) ?: return Velocity.Zero
                            listState.animateScrollToItem(target)
                            return available
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity,
                        ): Velocity {
                            val directionY = when {
                                consumed.y != 0f -> consumed.y
                                else -> available.y
                            }
                            heroSnapTarget(directionY)?.let { target ->
                                listState.animateScrollToItem(target)
                            }
                            return Velocity.Zero
                        }
                    }
                }
                var wheelScrollGeneration by remember(i) { mutableIntStateOf(0) }
                var wheelScrollDirectionY by remember(i) { mutableFloatStateOf(0f) }
                LaunchedEffect(wheelScrollGeneration, isCurrentItem, selectorMounted) {
                    if (wheelScrollGeneration == 0 || !isCurrentItem || selectorMounted) {
                        return@LaunchedEffect
                    }
                    delay(180.milliseconds)
                    snapshotFlow { listState.isScrollInProgress }.first { !it }
                    heroSnapTarget(wheelScrollDirectionY)?.let { target ->
                        listState.animateScrollToItem(target)
                    }
                }
                LazyColumn(
                    Modifier
                        .nestedScroll(heroSnapConnection)
                        .pointerInput(i, isCurrentItem, selectorMounted) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type != PointerEventType.Scroll) continue
                                    // Ctrl+wheel is handled by the desktop zoom control.
                                    if (event.changes.any { it.isConsumed }) continue
                                    val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (scrollY != 0f && isCurrentItem && !selectorMounted) {
                                        // Wheel down matches a negative fling velocity.
                                        wheelScrollDirectionY = if (scrollY > 0f) -1f else 1f
                                        wheelScrollGeneration++
                                    }
                                }
                            }
                        }
                        .paddingMask(safeDrawing) {
                            playerSheet?.progressState?.floatValue ?: 0f
                        },
                    state = listState,
                    contentPadding = PaddingValues(
                        top = topPadding,
                        start = safeDrawing.calculateStartPadding(layoutDirection),
                        end = safeDrawing.calculateEndPadding(layoutDirection),
                    )
                ) {
                    item(key = "player-hero-$i") {
                        PlayerHero(
                            i = i,
                            lyrics = selectedLyrics?.lyrics,
                            lyricsTimingIndex = selectedLyrics?.timingIndex,
                            showLyrics = showLyrics,
                            collapseTimelineRowsForLyrics = lyricsChromeCollapsed,
                            onCollapseTimelineRowsForLyricsChange = setLyricsChromeCollapsed,
                            onLyricsChromeInteraction = onLyricsChromeInteraction,
                            userScrollEnabled = isPlayerScrolledToTop,
                            topPadding = topPadding,
                            viewportWidth = coverViewportWidth,
                            transformModifier = transformModifier,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(heroHeight)
                                .graphicsLayer(alpha = heroAlpha),
                            onArtworkClick = {
                                if (sharedLyricsVisible != null) {
                                    sharedLyricsVisible.value = false
                                } else {
                                    fallbackShowLyrics = false
                                }
                            },
                        )
                    }
                    stickyHeader(bottomBarKey, PlayerBottomBarContentType, isSlidable = false) {
                        Box(
                            transformModifier
                                .blockDescendantBringIntoView()
                                .height(playerBottomBarHeight)
                        ) {
                            BottomBar(
                                index = i,
                                isSticky = isBottomBarSticky,
                                lyricsVisible = showLyrics,
                                selectedLyrics = selectedLyrics,
                                lyricsSelectorActive = lyricsMounted,
                                queueButtonHidden = queueMounted,
                                onLyricsPillBoundsChanged = { bounds ->
                                    lyricsPillCoordinates = bounds
                                    if (!isBottomBarSticky) {
                                        localPillBounds(bounds)?.let {
                                            lyricsTransition.updateRestoredAnchor(it)
                                        }
                                    }
                                },
                                onQueueButtonBoundsChanged = { bounds ->
                                    queueButtonCoordinates = bounds
                                    if (!isBottomBarSticky) {
                                        queuePillBounds()?.let {
                                            queueTransition.updateRestoredAnchor(it)
                                        }
                                    }
                                },
                                onLyricsSelectorClick = {
                                    if (isCurrentItem && showLyrics && !isBottomBarSticky) {
                                        localPillBounds(lyricsPillCoordinates)?.let {
                                            openLyrics(it)
                                        }
                                    }
                                },
                                onQueueButtonClick = {
                                    if (isCurrentItem && !isBottomBarSticky) {
                                        queuePillBounds()?.let {
                                            openQueue(it)
                                        }
                                    }
                                },
                                onLyricsClick = {
                                    listState.requestScrollToItem(0)
                                    if (sharedLyricsVisible != null) {
                                        sharedLyricsVisible.value = !sharedLyricsVisible.value
                                    } else {
                                        fallbackShowLyrics = !fallbackShowLyrics
                                    }
                                }
                            )
                        }
                    }

                    val firstHeaderKey = "Header $i"
                    stickyHeader(key = firstHeaderKey, contentType = "player-header") {
                        Box(
                            transformModifier.blockDescendantBringIntoView()
                                .clipHeaderTop(listState, firstHeaderKey)
                        ) {
                            Header(i.toString())
                        }
                    }
                    materialGroup(
                        lazyListState = listState,
                        clipPadding = PaddingValues(bottom = 8.dp + bottomPadding),
                    ) {
                        (0 until SongQueueItemsPerGroup).forEach {
                            card(
                                modifier = transformModifier.padding(horizontal = 8.dp),
                                key = "$i$it",
                                contentType = SongQueueItemContentType,
                                colors = cardColors
                            ) {
                                Box(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                playerSheet?.sheetState?.partialExpand()
                                                backStack?.add(Media(it.toString()))
                                            }
                                        }.padding(16.dp, 24.dp)
                                ) { Text("Item $it") }
                            }
                        }
                    }

                    val secondHeaderKey = "Header $i-2"
                    stickyHeader(key = secondHeaderKey, contentType = "player-header-2") {
                        Box(
                            transformModifier.blockDescendantBringIntoView()
                                .clipHeaderTop(listState, secondHeaderKey)
                        ) {
                            Header("$i 2")
                        }
                    }
                    materialGroup(
                        lazyListState = listState,
                        clipPadding = PaddingValues(bottom = 8.dp + bottomPadding),
                    ) {
                        (0 until SongQueueItemsPerGroup).forEach {
                            card(
                                modifier = transformModifier.padding(horizontal = 8.dp),
                                key = "$i$it 2",
                                contentType = SongQueueItemContentType,
                                colors = cardColors
                            ) {
                                Box(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                playerSheet?.sheetState?.partialExpand()
                                                backStack?.add(Media(it.toString()))
                                            }
                                        }.padding(16.dp, 24.dp)
                                ) { Text("Item $it") }
                            }
                        }
                    }
                }

                CollapsedPlayer(i = i, showCover = showLyrics)
                val scrollbarState = rememberPlayerScrollbarState(listState, scrollbarItemSizes)
                val scrollbarThumbMover =
                    rememberPlayerScrollbarThumbMover(listState, scrollbarItemSizes)
                var previousScrollbarTravel by remember(i) { mutableStateOf<Float?>(null) }
                var scrollbarDirectionY by remember(i) { mutableFloatStateOf(0f) }
                FastScrollbar(
                    modifier = transformModifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .padding(top = 4.dp, bottom = 4.dp)
                        .padding(safeDrawing)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            alpha = playerSheet?.progressState?.floatValue ?: 1f
                        }
                        .align(Alignment.TopEnd),
                    state = scrollbarState,
                    scrollInProgress = listState.isScrollInProgress,
                    orientation = Orientation.Vertical,
                    onThumbMoved = { travel ->
                        previousScrollbarTravel?.let { previous ->
                            val delta = travel - previous
                            if (delta != 0f) {
                                scrollbarDirectionY = if (delta < 0f) 1f else -1f
                            }
                        }
                        previousScrollbarTravel = travel
                        scrollbarThumbMover(travel)
                    },
                    onThumbDragFinished = {
                        val directionY = scrollbarDirectionY
                        previousScrollbarTravel = null
                        scrollbarDirectionY = 0f
                        heroSnapTarget(directionY)?.let { target ->
                            scope.launch { listState.animateScrollToItem(target) }
                        }
                    },
                )
            }
            if (lyricsMounted) {
                PlayerPillOverlay(
                    transition = lyricsTransition,
                    viewport = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                    isCurrentPage = isCurrentItem,
                    pillColor = colorScheme.primary.copy(alpha = 0.1f),
                    openInfo = LyricsSelectorPageInfo,
                    closedInfo = LyricsSelectorPlayerInfo,
                    alternateSource = if (isBottomBarSticky) null else queuePillBounds(),
                    onAlternateClick = {
                        if (!isBottomBarSticky) queuePillBounds()?.let(::openQueue)
                    },
                    pillContent = { LyricsSelectionPillContent(selectedLyrics) },
                    pageContent = { interactive ->
                        LyricsSelectionPage(
                            selectedLyrics = selectedLyrics,
                            items = lyricsSelections,
                            topPadding = topPadding,
                            interactive = interactive,
                            onSelect = { selected ->
                                val index = lyricsSelections.indexOf(selected)
                                if (index >= 0) selectedLyricsIndex = index
                                lyricsTransition.close()
                            },
                            onClearSelection = { selectedLyricsIndex = null },
                            onBack = { lyricsTransition.close() },
                        )
                    },
                )
            }
            if (queueMounted) {
                PlayerPillOverlay(
                    transition = queueTransition,
                    viewport = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                    isCurrentPage = isCurrentItem,
                    pillColor = Color.Transparent,
                    openInfo = QueueOverlayPageInfo,
                    closedInfo = QueuePlayerInfo,
                    alternateSource = if (showLyrics && !isBottomBarSticky) {
                        localPillBounds(lyricsPillCoordinates)
                    } else null,
                    onAlternateClick = {
                        if (showLyrics && !isBottomBarSticky) {
                            localPillBounds(lyricsPillCoordinates)?.let(::openLyrics)
                        }
                    },
                    pillContent = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_queue_music),
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                            )
                        }
                    },
                    pageContent = { interactive ->
                        QueuePage(
                            currentSong = i,
                            songCount = dev.brahmkshatriya.echo.app.ui.player.LocalPlayerItems.current.size,
                            topPadding = topPadding,
                            interactive = interactive,
                            onBack = { queueTransition.close() },
                            onSongSelected = { index ->
                                queueTransition.close()
                                scope.launch {
                                    snapshotFlow { queueTransition.mounted }.first { !it }
                                    pagerState?.animateScrollToPage(index)
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}
