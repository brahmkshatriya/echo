package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.playback.PlayerSession
import dev.brahmkshatriya.echo.app.ui.Media
import dev.brahmkshatriya.echo.app.ui.components.FastScrollbar
import dev.brahmkshatriya.echo.app.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.main.Header
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPagerState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPresentation
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerQueueTransition
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.PlayerBottomBarContentType
import dev.brahmkshatriya.echo.app.ui.player.PlayerPresentationMode
import dev.brahmkshatriya.echo.app.ui.player.PlayerScrollbarItemSizes
import dev.brahmkshatriya.echo.app.ui.player.blockDescendantBringIntoView
import dev.brahmkshatriya.echo.app.ui.player.clipHeaderTop
import dev.brahmkshatriya.echo.app.ui.player.components.CollapsedPlayer
import dev.brahmkshatriya.echo.app.ui.player.components.PlayerPillOverlay
import dev.brahmkshatriya.echo.app.ui.player.components.StickyMiniPlayer
import dev.brahmkshatriya.echo.app.ui.player.components.rememberPlayerPillTransition
import dev.brahmkshatriya.echo.app.ui.player.expandedListItemTransform
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarHeight
import dev.brahmkshatriya.echo.app.ui.player.redrawPlayerOnReveal
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerScrollbarState
import dev.brahmkshatriya.echo.app.ui.player.rememberPlayerScrollbarThumbMover
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPage
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPillContent
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectorPageInfo
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectorPlayerInfo
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberLyricsSelections
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SongPlayerItem(
    controls: PlayerSession,
    i: Int,
    onScrolledToTopChanged: (Boolean) -> Unit = {},
    onLyricsSelectorOpenChanged: (Boolean) -> Unit = {},
) =
    CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
        val pagerState = LocalPlayerPagerState.current
        val scope = rememberCoroutineScope()
        val isCurrentItem = pagerState == null || controls.currentEntryId == i
        val pageVisible by
            remember(pagerState, i) {
                derivedStateOf {
                    pagerState == null || pagerState.layoutInfo.visiblePagesInfo.any { it.key == i }
                }
            }
        val playerSheet = LocalPlayerSheet.current
        val prepareLyrics by
            remember(playerSheet) {
                derivedStateOf {
                    playerSheet == null ||
                        playerSheet.sheetState.targetValue == SheetValue.Expanded ||
                        playerSheet.progressState.value > 0f
                }
            }
        val lyricsSelections = rememberLyricsSelections(load = prepareLyrics)
        val timelineState = controls.timelineFor(i)
        val lyricsTransition = rememberPlayerPillTransition("lyrics")
        val queueTransition = LocalPlayerQueueTransition.current
        val overlays =
            remember(i, lyricsTransition, queueTransition) {
                SongPlayerOverlayState(lyricsTransition, queueTransition)
            }
        BoxWithConstraints {
            val immersiveContentVisible by
                remember(playerSheet) {
                    derivedStateOf { (playerSheet?.progressState?.value ?: 1f) > 0.5f }
                }
            val backStack = LocalMainBackStack.current
            val cardColors =
                CardDefaults.cardColors(
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface,
                )
            val listState = rememberLazyListState()
            val scrollbarItemSizes = remember { PlayerScrollbarItemSizes() }
            val bottomBarKey = remember(i) { "player-bottom-bar-$i" }
            val isBottomBarSticky by
                remember(bottomBarKey) {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val item =
                            layoutInfo.visibleItemsInfo.firstOrNull { it.key == bottomBarKey }
                        item != null &&
                            item.offset <= 0 &&
                            listState.firstVisibleItemIndex >= item.index
                    }
                }
            val presentation = LocalPlayerPresentation.current
            val showLyrics = presentation.mode == PlayerPresentationMode.Lyrics
            val showKineticShapes = presentation.mode == PlayerPresentationMode.Kinetic
            val immersiveMode = presentation.mode != PlayerPresentationMode.Cover
            val lyricsChromeCollapsed = presentation.chromeCollapsed
            val chrome =
                rememberSongPlayerChromeState(
                    entryId = i,
                    presentation = presentation,
                    isCurrentItem = isCurrentItem,
                    isSeeking = timelineState.isSeeking,
                    isBottomBarSticky = isBottomBarSticky,
                )
            val setLyricsChromeCollapsed: (Boolean) -> Unit = chrome::setCollapsed
            val onLyricsChromeInteraction: () -> Unit = chrome::interact
            // The current pager item owns both the morph and its two pill anchors.
            val mediaId = controls.mediaFor(i)?.id
            val selectedLyricsIndex = presentation.lyricsSource(mediaId)
            val setSelectedLyricsIndex: (Int?) -> Unit = { index ->
                mediaId?.let { presentation.selectLyricsSource(it, index) }
            }
            val selectedLyrics = selectedLyricsIndex?.let(lyricsSelections::getOrNull)
            val lyricsMounted = lyricsTransition.open || lyricsTransition.mounted
            val queueMounted = queueTransition.open || queueTransition.mounted
            val selectorMounted = lyricsMounted || queueMounted
            val pillDensity = LocalDensity.current
            fun queuePillBounds(
                anchor: LayoutCoordinates? = overlays.queueButtonCoordinates
            ): Rect? =
                overlays.queueBounds(pillDensity, IconButtonDefaults.smallContainerSize(), anchor)
            LaunchedEffect(
                overlays.playerCoordinates,
                overlays.lyricsPillCoordinates,
                overlays.queueButtonCoordinates,
                isBottomBarSticky,
                isCurrentItem,
            ) {
                if (!isBottomBarSticky) {
                    overlays
                        .localBounds(overlays.lyricsPillCoordinates)
                        ?.let(lyricsTransition::updateAnchor)
                    if (isCurrentItem) queuePillBounds()?.let(queueTransition::updateAnchor)
                }
            }
            SideEffect {
                if (isCurrentItem) {
                    queueTransition.alternateSource =
                        if (showLyrics && !isBottomBarSticky)
                            overlays.localBounds(overlays.lyricsPillCoordinates)
                        else null
                    queueTransition.onAlternateClick = {
                        if (showLyrics && !isBottomBarSticky)
                            overlays
                                .localBounds(overlays.lyricsPillCoordinates)
                                ?.let(overlays::openLyrics)
                    }
                }
            }
            val currentOnSelectorOpenChanged by rememberUpdatedState(onLyricsSelectorOpenChanged)
            LaunchedEffect(selectorMounted) { currentOnSelectorOpenChanged(selectorMounted) }
            LaunchedEffect(showLyrics) { if (!showLyrics) lyricsTransition.close() }
            val isPlayerScrolledToTop by remember {
                derivedStateOf { !listState.canScrollBackward }
            }
            val heroInViewport by
                remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
            LaunchedEffect(heroInViewport, isCurrentItem) {
                if (heroInViewport && isCurrentItem) onLyricsChromeInteraction()
            }
            SongPlayerBackHandler(
                entryId = i,
                listState = listState,
                scrollbarItemSizes = scrollbarItemSizes,
                isCurrentItem = isCurrentItem,
                selectorMounted = selectorMounted,
                isBottomBarSticky = isBottomBarSticky,
                viewportHeightPx = constraints.maxHeight,
            )
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
                    snapshotFlow { sheet.progressState.value < 0.75f }
                        .collect { shouldReset ->
                            if (shouldReset && listState.canScrollBackward) {
                                withFrameNanos {}
                                listState.scrollToItem(0)
                            }
                        }
                }
            }

            val playerViewportHeightPx = constraints.maxHeight
            val heroHeight =
                (maxHeight - topPadding - bottomPadding - playerBottomBarHeight).coerceAtLeast(0.dp)
            val heroHeightPx = with(LocalDensity.current) { heroHeight.toPx().coerceAtLeast(1f) }
            val heroScrollProgress by
                remember(listState, heroHeightPx) {
                    derivedStateOf {
                        val scrollOffset =
                            if (listState.firstVisibleItemIndex == 0)
                                listState.firstVisibleItemScrollOffset.toFloat()
                            else heroHeightPx
                        (scrollOffset / heroHeightPx).coerceIn(0f, 1f)
                    }
                }
            val heroVisible by
                remember(playerSheet, listState, heroHeightPx) {
                    derivedStateOf { immersiveContentVisible && heroScrollProgress < 0.6f }
                }
            val coverViewportWidth = maxWidth
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        val reveal =
                            maxOf(lyricsTransition.reveal.value, queueTransition.reveal.value)
                        val back =
                            when {
                                lyricsTransition.backGestureActive ->
                                    lyricsTransition.backProgress.value.coerceIn(0f, 1f)
                                queueTransition.backGestureActive ->
                                    queueTransition.backProgress.value.coerceIn(0f, 1f)
                                else -> 0f
                            }
                        val pageProgress = (reveal / 0.45f).coerceIn(0f, 1f)
                        val scale = 1f - 0.04f * pageProgress * (1f - back)
                        scaleX = scale
                        scaleY = scale
                    }
                    .onGloballyPositioned { overlays.updatePlayer(it) }
            ) {
                val transformModifier =
                    Modifier.expandedListItemTransform(playerSheet) { playerViewportHeightPx }

                fun heroSnapTarget(directionY: Float): Int? {
                    val hero =
                        listState.layoutInfo.visibleItemsInfo.firstOrNull {
                            it.key == "player-hero-$i"
                        } ?: return null
                    val offset = listState.firstVisibleItemScrollOffset
                    if (
                        listState.firstVisibleItemIndex != hero.index ||
                            offset <= 0 ||
                            hero.size <= 0
                    )
                        return null

                    return when {
                        directionY < 0f -> 1
                        directionY > 0f -> 0
                        offset >= hero.size / 8 -> 1
                        else -> 0
                    }
                }

                val heroSnapConnection =
                    remember(listState, i) {
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
                                val directionY =
                                    when {
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
                    if (wheelScrollGeneration == 0 || !isCurrentItem || selectorMounted)
                        return@LaunchedEffect
                    delay(180.milliseconds)
                    snapshotFlow { listState.isScrollInProgress }.first { !it }
                    heroSnapTarget(wheelScrollDirectionY)?.let { target ->
                        listState.animateScrollToItem(target)
                    }
                }
                Box(
                    Modifier.fillMaxSize()
                        .nestedScroll(heroSnapConnection)
                        .scrollable(
                            state = listState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                        )
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
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize().paddingMask(
                            safeDrawing,
                            backgroundColor = colorScheme.primaryContainer,
                        ) {
                            playerSheet?.progressState?.value ?: 0f
                        },
                        state = listState,
                        userScrollEnabled = false,
                        contentPadding =
                            PaddingValues(
                                top = topPadding,
                                start = safeDrawing.calculateStartPadding(layoutDirection),
                                end = safeDrawing.calculateEndPadding(layoutDirection),
                            ),
                    ) {
                        // Only list geometry is recycled; the hero and its page state live above
                        // the list.
                        item(key = "player-hero-$i") {
                            Spacer(Modifier.fillMaxWidth().height(heroHeight))
                        }
                        stickyHeader(bottomBarKey, PlayerBottomBarContentType, isSlidable = false) {
                            Box(
                                transformModifier
                                    .blockDescendantBringIntoView()
                                    .height(playerBottomBarHeight)
                            ) {
                                BottomBar(
                                    timelineState = timelineState,
                                    isPlaying = controls.isPlaying,
                                    miniPlayerContent = { StickyMiniPlayer(controls, i) },
                                    index = i,
                                    scrollProgress = { heroScrollProgress },
                                    lyricsVisible = showLyrics,
                                    lyricsChromeCollapsed = lyricsChromeCollapsed,
                                    selectedLyrics = selectedLyrics,
                                    lyricsSelectorActive = lyricsMounted,
                                    queueButtonHidden = queueMounted,
                                    onLyricsPillBoundsChanged = { bounds ->
                                        overlays.updateLyricsPill(bounds)
                                        if (!isBottomBarSticky)
                                            overlays.localBounds(bounds)?.let {
                                                lyricsTransition.updateAnchor(it)
                                            }
                                    },
                                    onQueueButtonBoundsChanged = { bounds ->
                                        overlays.updateQueueButton(bounds)
                                        if (isCurrentItem && !isBottomBarSticky)
                                            queuePillBounds(bounds)?.let {
                                                queueTransition.updateAnchor(it)
                                            }
                                    },
                                    onLyricsSelectorClick = {
                                        if (isCurrentItem && showLyrics && !isBottomBarSticky)
                                            overlays
                                                .localBounds(overlays.lyricsPillCoordinates)
                                                ?.let { overlays.openLyrics(it) }
                                    },
                                    onQueueButtonClick = {
                                        if (isCurrentItem && !isBottomBarSticky)
                                            queuePillBounds()?.let { overlays.openQueue(it) }
                                    },
                                    onLyricsClick = {
                                        listState.requestScrollToItem(0)
                                        if (showLyrics || showKineticShapes) {
                                            // Start the chrome with the mode change, whether
                                            // returning
                                            // to the cover or entering the other immersive view.
                                            setLyricsChromeCollapsed(false)
                                            chrome.restartTimeout()
                                        }

                                        presentation.show(
                                            if (showLyrics) PlayerPresentationMode.Cover
                                            else PlayerPresentationMode.Lyrics
                                        )
                                    },
                                )
                            }
                        }

                        val firstHeaderKey = "Header $i"
                        stickyHeader(key = firstHeaderKey, contentType = "player-header") {
                            Box(
                                transformModifier
                                    .blockDescendantBringIntoView()
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
                                    colors = cardColors,
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    playerSheet?.sheetState?.partialExpand()
                                                    backStack?.add(Media(it.toString()))
                                                }
                                            }
                                            .padding(16.dp, 24.dp)
                                    ) {
                                        Text("Item $it")
                                    }
                                }
                            }
                        }

                        val secondHeaderKey = "Header $i-2"
                        stickyHeader(key = secondHeaderKey, contentType = "player-header-2") {
                            Box(
                                transformModifier
                                    .blockDescendantBringIntoView()
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
                                    colors = cardColors,
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    playerSheet?.sheetState?.partialExpand()
                                                    backStack?.add(Media(it.toString()))
                                                }
                                            }
                                            .padding(16.dp, 24.dp)
                                    ) {
                                        Text("Item $it")
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        Modifier.fillMaxSize().paddingMask(
                            safeDrawing,
                            backgroundColor = colorScheme.primaryContainer,
                        ) {
                            playerSheet?.progressState?.value ?: 0f
                        }
                    ) {
                        PlayerHero(
                            media = controls.mediaFor(i),
                            timelineState = timelineState,
                            isPlaying = controls.isPlaying,
                            bassLevel = controls.bassLevel,
                            onSeek = { controls.seekTo(it, i) },
                            timelineContent = {
                                geometry,
                                rowsCollapsed,
                                compactArtwork,
                                collapseProgress ->
                                ExpandedTimeline(
                                    session = controls,
                                    i = i,
                                    geometry = geometry,
                                    rowsCollapsed = rowsCollapsed,
                                    lyricsVisible = immersiveMode,
                                    showCompactArtwork = compactArtwork,
                                    rowsCollapseProgress = collapseProgress,
                                    onArtworkClick = {
                                        setLyricsChromeCollapsed(false)
                                        presentation.show(PlayerPresentationMode.Cover)
                                    },
                                )
                            },
                            lyrics = selectedLyrics?.lyrics,
                            lyricsTimingIndex = selectedLyrics?.timingIndex,
                            showLyrics = showLyrics,
                            showKineticShapes = showKineticShapes,
                            pageActive = pageVisible,
                            immersiveContentActive =
                                isCurrentItem && !isBottomBarSticky && immersiveContentVisible,
                            collapseTimelineRowsForLyrics = lyricsChromeCollapsed,
                            onCollapseTimelineRowsForLyricsChange = setLyricsChromeCollapsed,
                            onLyricsChromeInteraction = onLyricsChromeInteraction,
                            onKineticShapesChromeClick = {
                                if (isCurrentItem && showKineticShapes) {
                                    setLyricsChromeCollapsed(!lyricsChromeCollapsed)
                                    chrome.restartTimeout()
                                }
                            },
                            userScrollEnabled = isPlayerScrolledToTop,
                            topPadding = topPadding,
                            viewportWidth = coverViewportWidth,
                            transformModifier = transformModifier,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        start = safeDrawing.calculateStartPadding(layoutDirection),
                                        end = safeDrawing.calculateEndPadding(layoutDirection),
                                    )
                                    .offset {
                                        val scroll =
                                            if (listState.firstVisibleItemIndex == 0)
                                                listState.firstVisibleItemScrollOffset.toFloat()
                                            else heroHeightPx
                                        IntOffset(0, (topPadding.toPx() - scroll).roundToInt())
                                    }
                                    .height(heroHeight)
                                    .graphicsLayer {
                                        alpha = (1f - heroScrollProgress / 0.6f).coerceIn(0f, 1f)
                                    }
                                    .redrawPlayerOnReveal(heroVisible),
                            onLargeArtworkClick = {
                                if (isCurrentItem) {
                                    presentation.show(PlayerPresentationMode.Kinetic)
                                    setLyricsChromeCollapsed(false)
                                    chrome.restartTimeout()
                                }
                            },
                        )
                    }
                }

                CollapsedPlayer(session = controls, i = i, showCover = immersiveMode)
                val scrollbarState = rememberPlayerScrollbarState(listState, scrollbarItemSizes)
                val scrollbarThumbMover =
                    rememberPlayerScrollbarThumbMover(listState, scrollbarItemSizes)
                var previousScrollbarTravel by remember(i) { mutableStateOf<Float?>(null) }
                var scrollbarDirectionY by remember(i) { mutableFloatStateOf(0f) }
                FastScrollbar(
                    modifier =
                        transformModifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .padding(top = 4.dp, bottom = 4.dp)
                            .padding(safeDrawing)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                                alpha = playerSheet?.progressState?.value ?: 1f
                            }
                            .align(Alignment.TopEnd),
                    state = scrollbarState,
                    scrollInProgress = listState.isScrollInProgress,
                    orientation = Orientation.Vertical,
                    onThumbMoved = { travel ->
                        previousScrollbarTravel?.let { previous ->
                            val delta = travel - previous
                            if (delta != 0f) scrollbarDirectionY = if (delta < 0f) 1f else -1f
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
            if (lyricsMounted)
                PlayerPillOverlay(
                    transition = lyricsTransition,
                    viewport =
                        Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                    isCurrentPage = isCurrentItem,
                    pillColor = colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    openInfo = LyricsSelectorPageInfo,
                    closedInfo = LyricsSelectorPlayerInfo,
                    currentSource = { overlays.localBounds(overlays.lyricsPillCoordinates) },
                    alternateSource = if (isBottomBarSticky) null else queuePillBounds(),
                    onAlternateClick = {
                        if (!isBottomBarSticky) queuePillBounds()?.let(overlays::openQueue)
                    },
                    pillContent = {
                        LyricsSelectionPillContent(
                            selectedLyrics = selectedLyrics,
                            chromeCollapsed = showLyrics && lyricsChromeCollapsed,
                        )
                    },
                    pageContent = { interactive ->
                        LyricsSelectionPage(
                            selectedLyrics = selectedLyrics,
                            items = lyricsSelections,
                            topPadding = topPadding,
                            interactive = interactive,
                            onSelect = { selected ->
                                val index = lyricsSelections.indexOf(selected)
                                if (index >= 0) setSelectedLyricsIndex(index)
                                lyricsTransition.close()
                            },
                            onClearSelection = { setSelectedLyricsIndex(null) },
                            onBack = { lyricsTransition.close() },
                        )
                    },
                )
        }
    }

/**
 * The queue's only composition is outside the pager, so moving the playing song's pager key cannot
 * unmount the queue for a frame.
 */
