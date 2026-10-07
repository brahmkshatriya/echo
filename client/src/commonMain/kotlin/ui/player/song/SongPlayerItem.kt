package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
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
import dev.brahmkshatriya.echo.app.ui.player.blockDescendantBringIntoView
import dev.brahmkshatriya.echo.app.ui.player.clipHeaderTop
import dev.brahmkshatriya.echo.app.ui.player.expandedListItemTransform
import dev.brahmkshatriya.echo.app.ui.player.playNext
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarControlSize
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarHeight
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerScrollbarState
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerScrollbarThumbMover
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPage
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPillContent
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectorPageInfo
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectorPlayerInfo
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberLyricsSelections
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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
    var playerBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    CompositionLocalProvider(LocalPlayerTimelineState provides timelineState) {
        BoxWithConstraints(
            Modifier.onGloballyPositioned { playerBoundsInRoot = it.boundsInRoot() }
        ) {
            val playerSheet = LocalPlayerSheet.current
            val backStack = LocalMainBackStack.current
            val cardColors = CardDefaults.cardColors(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface
            )
            val listState = rememberLazyListState()
            val bottomBarKey = remember(i) { "player-bottom-bar-$i" }
            val isBottomBarSticky by remember(bottomBarKey) {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val item =
                        layoutInfo.visibleItemsInfo.firstOrNull { it.key == bottomBarKey }
                    item != null && item.offset <= 0 && listState.firstVisibleItemIndex >= item.index
                }
            }
            var fallbackShowLyrics by remember { mutableStateOf(false) }
            val sharedLyricsVisible = LocalPlayerLyricsVisible.current
            val showLyrics = sharedLyricsVisible?.value ?: fallbackShowLyrics
            var fallbackLyricsChromeCollapsed by remember { mutableStateOf(false) }
            val sharedLyricsChromeCollapsed = LocalPlayerLyricsChromeCollapsed.current
            val lyricsChromeCollapsed =
                sharedLyricsChromeCollapsed?.value ?: fallbackLyricsChromeCollapsed
            val setLyricsChromeCollapsed: (Boolean) -> Unit = { collapsed ->
                if (isCurrentItem) {
                    val effectiveCollapsed = collapsed && !isBottomBarSticky
                    if (sharedLyricsChromeCollapsed != null) {
                        sharedLyricsChromeCollapsed.value = effectiveCollapsed
                    } else {
                        fallbackLyricsChromeCollapsed = effectiveCollapsed
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
            LaunchedEffect(isBottomBarSticky) {
                if (isBottomBarSticky && lyricsChromeCollapsed) {
                    setLyricsChromeCollapsed(false)
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
            var selectedLyricsIndex by remember(i) { mutableStateOf<Int?>(0) }
            val selectedLyrics = selectedLyricsIndex?.let { index ->
                lyricsSelections.getOrNull(index)
            }
            var showSelector by remember(i) { mutableStateOf(false) }
            var lyricsPillBoundsInRoot by remember(i) { mutableStateOf<Rect?>(null) }
            var selectorRevealSourceBounds by remember(i) { mutableStateOf<Rect?>(null) }
            val selectorRevealProgress = remember(i) { Animatable(0f) }
            val selectorPillCenter = remember(i) {
                Animatable(Offset.Zero, Offset.VectorConverter)
            }
            val selectorPillSize = remember(i) {
                Animatable(Size.Zero, Size.VectorConverter)
            }
            val selectorPillRadius = remember(i) { Animatable(0f) }
            val selectorContentScale = remember(i) {
                Animatable(0.66f, visibilityThreshold = 0.0001f)
            }
            var selectorMounted by remember(i) { mutableStateOf(false) }
            val selectorRevealAnimationSpec = motionScheme.defaultSpatialSpec<Float>()
            val selectorPositionAnimationSpec = motionScheme.defaultSpatialSpec<Offset>()
            val selectorSizeAnimationSpec = motionScheme.defaultEffectsSpec<Size>()
            val selectorRadiusAnimationSpec = motionScheme.defaultSpatialSpec<Float>()
            val selectorPredictiveBackProgress = remember(i) { Animatable(0f) }
            val selectorTransitionDensity = LocalDensity.current
            val currentSelectorRevealSourceBounds = lyricsPillBoundsInRoot?.let { pillBounds ->
                playerBoundsInRoot?.let { playerBounds ->
                    Rect(
                        left = pillBounds.left - playerBounds.left,
                        top = pillBounds.top - playerBounds.top,
                        right = pillBounds.right - playerBounds.left,
                        bottom = pillBounds.bottom - playerBounds.top,
                    )
                }
            } ?: selectorRevealSourceBounds

            val selectorNavigationEventState = rememberNavigationEventState(
                currentInfo = if (showSelector) LyricsSelectorPageInfo else LyricsSelectorPlayerInfo,
                backInfo = if (showSelector) listOf(LyricsSelectorPlayerInfo) else emptyList(),
            )
            LaunchedEffect(selectorNavigationEventState) {
                snapshotFlow {
                    when (val state = selectorNavigationEventState.transitionState) {
                        NavigationEventTransitionState.Idle -> null
                        is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
                    }
                }.collectLatest { progress ->
                    if (progress != null) {
                        selectorPredictiveBackProgress.snapTo(
                            progress.coerceIn(0f, 1f)
                        )
                    }
                }
            }
            if (LocalNavigationEventDispatcherOwner.current != null) {
                NavigationBackHandler(
                    state = selectorNavigationEventState,
                    isBackEnabled = showSelector,
                    onBackCompleted = {
                        showSelector = false
                        scope.launch {
                            selectorPredictiveBackProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                    },
                    onBackCancelled = {
                        scope.launch {
                            selectorPredictiveBackProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                    },
                )
            }
            LaunchedEffect(
                showSelector,
                currentSelectorRevealSourceBounds,
                constraints.maxWidth,
                constraints.maxHeight,
            ) {
                if (showSelector) {
                    val sourceBounds = currentSelectorRevealSourceBounds ?: return@LaunchedEffect
                    if (!selectorMounted) {
                        selectorMounted = true
                        selectorPillCenter.snapTo(sourceBounds.center)
                        selectorPillSize.snapTo(sourceBounds.size)
                        selectorPillRadius.snapTo(
                            with(selectorTransitionDensity) {
                                (playerBottomBarControlSize / 2f).toPx()
                            }
                        )
                    }
                    coroutineScope {
                        launch {
                            selectorPillCenter.animateTo(
                                targetValue = Offset(
                                    x = constraints.maxWidth / 2f,
                                    y = constraints.maxHeight / 2f,
                                ),
                                animationSpec = selectorPositionAnimationSpec,
                            )
                        }
                        launch {
                            selectorPillSize.animateTo(
                                targetValue = Size(
                                    width = constraints.maxWidth.toFloat(),
                                    height = constraints.maxHeight.toFloat(),
                                ),
                                animationSpec = selectorSizeAnimationSpec,
                            )
                        }
                        launch {
                            selectorPillRadius.animateTo(
                                targetValue = 0f,
                                animationSpec = selectorRadiusAnimationSpec,
                            )
                        }
                        launch {
                            selectorRevealProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                        launch {
                            selectorContentScale.animateTo(
                                targetValue = 1f,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                    }
                } else if (selectorMounted) {
                    val sourceCenter = currentSelectorRevealSourceBounds?.center
                    coroutineScope {
                        launch {
                            selectorPillCenter.animateTo(
                                targetValue = sourceCenter ?: selectorPillCenter.value,
                                animationSpec = selectorPositionAnimationSpec,
                            )
                        }
                        launch {
                            selectorPillSize.animateTo(
                                targetValue = currentSelectorRevealSourceBounds?.size
                                    ?: selectorPillSize.value,
                                animationSpec = selectorSizeAnimationSpec,
                            )
                        }
                        launch {
                            selectorPillRadius.animateTo(
                                targetValue = with(selectorTransitionDensity) {
                                    (playerBottomBarControlSize / 2f).toPx()
                                },
                                animationSpec = selectorRadiusAnimationSpec,
                            )
                        }
                        launch {
                            selectorRevealProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                        launch {
                            selectorContentScale.animateTo(
                                targetValue = 0.66f,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                    }
                    selectorMounted = false
                }
            }
            LaunchedEffect(selectorMounted) {
                onLyricsSelectorOpenChanged(selectorMounted)
            }
            LaunchedEffect(showLyrics) {
                if (!showLyrics) showSelector = false
            }
            val isPlayerScrolledToTop by remember {
                derivedStateOf { !listState.canScrollBackward }
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

            val selectorProgress = selectorRevealProgress.value
            run {
                val playerViewportHeightPx = constraints.maxHeight
                val transformModifier = Modifier.expandedListItemTransform(playerSheet) {
                    playerViewportHeightPx
                }
                val heroHeight = (maxHeight - topPadding - bottomPadding - playerBottomBarHeight)
                    .coerceAtLeast(0.dp)
                val coverViewportWidth = maxWidth
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
                LazyColumn(
                    Modifier
                        .nestedScroll(heroSnapConnection)
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
                                .height(heroHeight),
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
                                lyricsSelectorActive = selectorMounted,
                                onLyricsPillBoundsChanged = { lyricsPillBoundsInRoot = it },
                                onLyricsSelectorClick = {
                                    selectorRevealSourceBounds = currentSelectorRevealSourceBounds
                                    showSelector = true
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
                val scrollbarItemSizes = remember { PlayerScrollbarItemSizes() }
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
            if (selectorMounted) {
                val density = LocalDensity.current
                val pillContentAlpha =
                    (1f - selectorProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
                val pageContentAlpha = ((selectorProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                val pageContentScale = selectorContentScale.value
                val animatedBounds = currentSelectorRevealSourceBounds?.let {
                    val center = selectorPillCenter.value
                    val size = selectorPillSize.value
                    Rect(
                        left = center.x - size.width / 2f,
                        top = center.y - size.height / 2f,
                        right = center.x + size.width / 2f,
                        bottom = center.y + size.height / 2f,
                    )
                }
                val pillContentBounds = currentSelectorRevealSourceBounds?.let { source ->
                    val center = selectorPillCenter.value
                    Rect(
                        left = center.x - source.width / 2f,
                        top = center.y - source.height / 2f,
                        right = center.x + source.width / 2f,
                        bottom = center.y + source.height / 2f,
                    )
                }
                val containerColor = lerp(
                    colorScheme.primary.copy(alpha = 0.1f),
                    colorScheme.surfaceContainer,
                    selectorProgress.coerceIn(0f, 1f),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (selectorPredictiveBackProgress.value.coerceIn(0f, 1f) > 0f) {
                                Modifier.graphicsLayer {
                                    val backProgress =
                                        selectorPredictiveBackProgress.value.coerceIn(0f, 1f)
                                    val scale = 1f - 0.15f * backProgress
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin =
                                        currentSelectorRevealSourceBounds?.let { bounds ->
                                            TransformOrigin(
                                                pivotFractionX = if (size.width > 0f) {
                                                    ((bounds.left + bounds.right) / 2f) / size.width
                                                } else 0.5f,
                                                pivotFractionY = if (size.height > 0f) {
                                                    ((bounds.top + bounds.bottom) / 2f) / size.height
                                                } else 1f,
                                            )
                                        } ?: TransformOrigin(0.5f, 1f)
                                    shape = RoundedCornerShape(28.dp * backProgress)
                                    clip = true
                                }
                            } else Modifier
                        )
                ) {
                    animatedBounds?.let { bounds ->
                        Box(
                            Modifier
                                .offset {
                                    IntOffset(
                                        bounds.left.roundToInt(),
                                        bounds.top.roundToInt(),
                                    )
                                }
                                .size(
                                    width = with(density) { bounds.width.toDp() },
                                    height = with(density) { bounds.height.toDp() },
                                )
                                .clip(
                                    RoundedCornerShape(
                                        with(density) {
                                            selectorPillRadius.value.coerceAtLeast(0f).toDp()
                                        }
                                    )
                                )
                                .background(containerColor)
                        )
                    }

                    if (pillContentAlpha > 0f) {
                        pillContentBounds?.let { bounds ->
                            LyricsSelectionPillContent(
                                selectedLyrics = selectedLyrics,
                                modifier = Modifier
                                    .size(
                                        width = with(density) { bounds.width.toDp() },
                                        height = with(density) { bounds.height.toDp() },
                                    )
                                    .graphicsLayer {
                                        translationX = bounds.left
                                        translationY = bounds.top
                                        alpha = pillContentAlpha
                                    },
                            )
                        }
                    }

                    Box(
                        if (!selectorRevealProgress.isRunning &&
                            !selectorContentScale.isRunning &&
                            pageContentAlpha >= 0.999f
                        ) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = pageContentAlpha
                                    scaleX = pageContentScale
                                    scaleY = pageContentScale
                                }
                        }
                    ) {
                        LyricsSelectionPage(
                            selectedLyrics = selectedLyrics,
                            items = lyricsSelections,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            onSelect = { selection ->
                                val index = lyricsSelections.indexOf(selection)
                                if (index >= 0) {
                                    selectedLyricsIndex = index
                                    showSelector = false
                                }
                            },
                            onClearSelection = { selectedLyricsIndex = null },
                            onBack = { showSelector = false },
                        )
                    }
                }
            }
        }
    }
}
