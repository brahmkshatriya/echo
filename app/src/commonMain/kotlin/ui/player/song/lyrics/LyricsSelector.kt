package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.ScaledTopAppBar
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarControlSize
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarInset
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarItemSpacing
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_back
import echo.app.generated.resources.ic_check_circle
import echo.app.generated.resources.ic_close_small
import echo.app.generated.resources.ic_playlist_remove
import echo.app.generated.resources.ic_search_outline
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
    bottomPadding: Dp,
    onSelect: (LyricsSelectionItem) -> Unit,
    onClearSelection: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = colorScheme.onSurface,
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
                            enabled = selectedLyrics != null,
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
                    query = query,
                    onQueryChange = { query = it },
                    onClear = {
                        query = ""
                        appliedQuery = ""
                    },
                    onSearch = { appliedQuery = query.trim() },
                    onBack = onBack,
                    bottomPadding = bottomPadding,
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

@Composable
private fun LyricsSelectionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = playerBottomBarInset,
                end = playerBottomBarInset,
                top = playerBottomBarInset,
                bottom = bottomPadding + playerBottomBarInset,
            ),
        horizontalArrangement = Arrangement.spacedBy(playerBottomBarItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = "Back",
            )
        }
        Row(
            modifier = Modifier
                .height(playerBottomBarControlSize)
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colorScheme.primary.copy(alpha = 0.08f))
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search lyrics",
                        style = typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.key) {
                                Key.Escape -> {
                                    focusManager.clearFocus()
                                    true
                                }

                                Key.Enter -> {
                                    onSearch()
                                    true
                                }

                                else -> false
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    textStyle = typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = Brush.verticalGradient(
                        listOf(colorScheme.primary, colorScheme.primary)
                    ),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
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
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_search_outline),
                    contentDescription = "Search lyrics",
                )
            }
        }
        IconButton(
            onClick = { },
            shapes = IconButtonDefaults.shapes()
        ) {
            BetterImage(
                model = { "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN" },
                "Spotify",
                modifier = Modifier.padding(4.dp).clip(Circle.toShape())
            )
        }
    }
}
