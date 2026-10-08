package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import dev.brahmkshatriya.echo.app.platform.imeAnimationInsetsOrNull
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.ScaledTopAppBar
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarControlSize
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarInset
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarItemSpacing
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_back
import echo.client.generated.resources.ic_check_circle
import echo.client.generated.resources.ic_chevron_backward
import echo.client.generated.resources.ic_close_small
import echo.client.generated.resources.ic_playlist_remove
import echo.client.generated.resources.ic_search_outline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.painterResource

internal object LyricsSelectorPlayerInfo : NavigationEventInfo()
internal object LyricsSelectorPageInfo : NavigationEventInfo()

internal val LyricsPosition.textAlign: TextAlign
    get() = when (this) {
        LyricsPosition.Start -> TextAlign.Start
        LyricsPosition.End -> TextAlign.End
    }


@Composable
internal fun LyricsSelectionPillContent(
    selectedLyrics: LyricsSelectionItem?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (selectedLyrics != null) {
            Text(
                text = selectedLyrics.title,
                style = typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedLyrics.extension,
                style = typography.labelSmall,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = "Choose Lyrics",
                style = typography.labelLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsSelectionPage(
    selectedLyrics: LyricsSelectionItem?,
    items: List<LyricsSelectionItem>,
    topPadding: Dp,
    interactive: Boolean = true,
    onSelect: (LyricsSelectionItem) -> Unit,
    onClearSelection: () -> Unit,
    onBack: () -> Unit,
) {
    val queryState = rememberTextFieldState()
    var appliedQuery by remember { mutableStateOf("") }
    val filteredItems = remember(items, appliedQuery) {
        val needle = appliedQuery.trim()
        if (needle.isEmpty()) {
            items
        } else {
            items.filter { item ->
                item.title.contains(needle, ignoreCase = true) ||
                        item.subtitle.contains(needle, ignoreCase = true) ||
                        item.extension.contains(needle, ignoreCase = true)
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val resultsListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurface,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                ScaledTopAppBar(
                    modifier = Modifier.padding(top = topPadding),
                    title = selectedLyrics?.title ?: "No lyrics selected",
                    subtitle = selectedLyrics?.subtitle ?: "Choose a lyrics source",
                    actions = {
                        IconButton(
                            onClick = onClearSelection,
                            enabled = interactive && selectedLyrics != null,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_playlist_remove),
                                contentDescription = "Deselect lyrics",
                            )
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                LyricsSelectionSearchBar(
                    queryState = queryState,
                    onClear = {
                        queryState.clearText()
                        appliedQuery = ""
                    },
                    onSearch = { appliedQuery = queryState.text.toString().trim() },
                    onBack = onBack,
                    interactive = interactive,
                )
            },
        ) { contentPadding ->
            val cardColors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            )
            LazyColumn(
                state = resultsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(horizontal = 8.dp),
                reverseLayout = true,
                userScrollEnabled = interactive,
                verticalArrangement = Arrangement.Bottom,
            ) {
                materialGroup(
                    lazyListState = resultsListState,
                    gap = 3.dp,
                    reverseLayout = true,
                ) {
                    filteredItems.forEach { item ->
                        card(
                            key = "${item.extension}:${item.title}:${item.subtitle}",
                            contentType = "lyrics-selection",
                            colors = cardColors
                        ) {
                            LyricsSelectionRow(
                                item = item,
                                selected = item == selectedLyrics,
                                enabled = interactive,
                                onClick = { onSelect(item) },
                            )
                        }
                    }
                }
                if (filteredItems.isEmpty()) {
                    item {
                        Text(
                            text = "No lyrics found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            style = typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsSelectionRow(
    item: LyricsSelectionItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = colorScheme.onSurface,
            )
            Text(
                text = item.subtitle.substringAfter(" • "),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check_circle),
                contentDescription = "Selected lyrics",
                modifier = Modifier.size(24.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsSelectionSearchBar(
    modifier: Modifier = Modifier,
    queryState: TextFieldState,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    interactive: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    // Animation source/target insets are exposed by Android only. On desktop
    // and other platforms, fall back to the normal IME inset.
    val platformImeAnimationInsets = imeAnimationInsetsOrNull()
    val imeAnimationSource = platformImeAnimationInsets?.first ?: imeInsets
    val imeAnimationTarget = platformImeAnimationInsets?.second ?: imeInsets
    val focusCloseMutex = remember { Mutex() }
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    var fieldEnabled by remember { mutableStateOf(interactive) }
    val extensionName = "Spotify"
    val widthMotion = motionScheme.fastSpatialSpec<IntSize>()
    val fadeMotion = motionScheme.fastEffectsSpec<Float>()
    val searchButtonColors = if (isFocused) {
        IconButtonDefaults.filledIconButtonColors()
    } else {
        IconButtonDefaults.iconButtonColors()
    }
    val searchButtonContainerColor by animateColorAsState(
        targetValue = searchButtonColors.containerColor,
        animationSpec = motionScheme.fastEffectsSpec(),
        label = "LyricsSearchButtonContainer",
    )
    val searchButtonContentColor by animateColorAsState(
        targetValue = searchButtonColors.contentColor,
        animationSpec = motionScheme.fastEffectsSpec(),
        label = "LyricsSearchButtonIconColor",
    )

    suspend fun clearSearchFocusAfterImeCloses() {
        focusCloseMutex.withLock {
            if (imeInsets.getBottom(density) == 0) {
                focusManager.clearFocus(force = true)
                return@withLock
            }

            keyboardController?.hide()
            snapshotFlow {
                imeInsets.getBottom(density) == 0 &&
                        imeAnimationSource.getBottom(density) == 0 &&
                        imeAnimationTarget.getBottom(density) == 0
            }.first { it }
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(interactive) {
        if (!interactive) {
            clearSearchFocusAfterImeCloses()
            fieldEnabled = false
        } else {
            fieldEnabled = true
        }
    }

    LaunchedEffect(isFocused, interactive) {
        if (isFocused && interactive) {
            var imeWasVisible = false
            snapshotFlow {
                Triple<Int, Int, Int>(
                    imeInsets.getBottom(density),
                    imeAnimationSource.getBottom(density),
                    imeAnimationTarget.getBottom(density),
                )
            }.collect { (height, _, target) ->
                if (height > 0) imeWasVisible = true
                if (imeWasVisible && height == 0 && target == 0) {
                    focusManager.clearFocus(force = true)
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = playerBottomBarInset,
                end = playerBottomBarInset,
                top = playerBottomBarInset,
                bottom = playerBottomBarInset,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = !isFocused,
            enter = fadeIn(animationSpec = fadeMotion) +
                    expandHorizontally(animationSpec = widthMotion, expandFrom = Alignment.Start),
            exit = fadeOut(animationSpec = fadeMotion) +
                    shrinkHorizontally(
                        animationSpec = widthMotion,
                        shrinkTowards = Alignment.Start
                    ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    enabled = interactive,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back),
                        contentDescription = "Back",
                    )
                }
                Box(Modifier.size(playerBottomBarItemSpacing))
            }
        }

        Row(
            modifier = Modifier
                .height(playerBottomBarControlSize)
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colorScheme.primary.copy(alpha = 0.08f))
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = fadeMotion) +
                        expandHorizontally(
                            animationSpec = widthMotion,
                            expandFrom = Alignment.Start
                        ),
                exit = fadeOut(animationSpec = fadeMotion) +
                        shrinkHorizontally(
                            animationSpec = widthMotion,
                            shrinkTowards = Alignment.Start
                        ),
            ) {
                IconButton(
                    onClick = { scope.launch { clearSearchFocusAfterImeCloses() } },
                    enabled = interactive,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_chevron_backward),
                        contentDescription = "Exit search",
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (queryState.text.isEmpty()) {
                    Crossfade(
                        targetState = isFocused,
                        animationSpec = fadeMotion,
                        label = "LyricsSearchPlaceholder",
                    ) { focused ->
                        Text(
                            text = if (focused) "Search lyrics" else extensionName,
                            modifier = Modifier.padding(start = if (focused) 0.dp else 8.dp),
                            style = typography.bodyLarge,
                            color = if (focused) {
                                colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                BasicTextField(
                    state = queryState,
                    enabled = fieldEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.key) {
                                Key.Escape -> {
                                    scope.launch { clearSearchFocusAfterImeCloses() }
                                    true
                                }

                                Key.Enter -> {
                                    onSearch()
                                    true
                                }

                                else -> false
                            }
                        },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = { onSearch() },
                    textStyle = typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.onSurface),
                )
            }

            if (queryState.text.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    enabled = interactive,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close_small),
                        contentDescription = "Clear search",
                    )
                }
            }

            IconButton(
                onClick = onSearch,
                enabled = interactive,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = searchButtonContainerColor,
                    contentColor = searchButtonContentColor,
                ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_search_outline),
                    contentDescription = "Search lyrics",
                )
            }
        }

        AnimatedVisibility(
            visible = !isFocused,
            enter = fadeIn(animationSpec = fadeMotion) +
                    expandHorizontally(animationSpec = widthMotion, expandFrom = Alignment.End),
            exit = fadeOut(animationSpec = fadeMotion) +
                    shrinkHorizontally(animationSpec = widthMotion, shrinkTowards = Alignment.End),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(playerBottomBarItemSpacing))
                IconButton(
                    onClick = { },
                    enabled = interactive,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    BetterImage(
                        model = {
                            "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN"
                        },
                        contentDescription = extensionName,
                        modifier = Modifier.padding(4.dp).clip(Circle.toShape()),
                    )
                }
            }
        }
    }
}
