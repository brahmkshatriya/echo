package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.PolygonIconRipple
import dev.brahmkshatriya.echo.app.ui.components.animatedPolygonIconShape
import dev.brahmkshatriya.echo.app.ui.components.clover6Sided
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_back
import echo.client.generated.resources.ic_drag_handle
import echo.client.generated.resources.ic_edit
import echo.client.generated.resources.ic_playlist_play
import echo.client.generated.resources.ic_repeat
import echo.client.generated.resources.ic_repeat_on
import echo.client.generated.resources.ic_repeat_one_on
import echo.client.generated.resources.ic_sensors
import echo.client.generated.resources.ic_shuffle
import echo.client.generated.resources.ic_shuffle_on
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun QueuePage(
    currentSong: Int,
    songCount: Int,
    onBack: () -> Unit,
    onSongSelected: (Int) -> Unit,
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val controls = LocalPlayerControls.current
    val songsInOrder = controls?.playbackOrder ?: (0 until songCount).toList()
    val listState = rememberLazyListState()
    var draggedSong by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val cardColors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            QueueActionBar(
                interactive,
                onBack,
                currentSong,
                safeDrawing.calculateBottomPadding()
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
            userScrollEnabled = interactive && draggedSong == null,
        ) {
            stickyHeader("queue-header") {
                TopAppBar(
                    title = { Text("Queue (${songCount + 1})") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            }
            if (songCount > 0) {
                materialGroup(
                    lazyListState = listState,
                    roundedCornerRadius = 20.dp,
                    gap = 3.dp
                ) {
                    songsInOrder.forEach { song ->
                        card(
                            key = "queue-song-${song}",
                            contentType = "queue-song",
                            colors = cardColors,
                            modifier = Modifier.padding(horizontal = 12.dp)
                                .zIndex(if (draggedSong == song) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (draggedSong == song) dragOffsetPx else 0f
                                },
                        ) {
                            QueueSongRow(
                                index = song,
                                playing = song == currentSong,
                                enabled = interactive,
                                onClick = { if (song != currentSong) onSongSelected(song) },
                                onDragStart = {
                                    draggedSong = song
                                    dragOffsetPx = 0f
                                },
                                onDrag = { delta ->
                                    dragOffsetPx += delta
                                    val items = listState.layoutInfo.visibleItemsInfo
                                    val dragged =
                                        items.firstOrNull { it.key == "queue-song-${song}" }
                                    if (dragged != null) {
                                        val center =
                                            dragged.offset + dragged.size / 2f + dragOffsetPx
                                        val target = items.firstOrNull { item ->
                                            item.key != dragged.key &&
                                                    item.key is String &&
                                                    (item.key as String).startsWith("queue-song-") &&
                                                    center >= item.offset && center < item.offset + item.size
                                        }
                                        val targetSong = (target?.key as? String)
                                            ?.removePrefix("queue-song-")?.toIntOrNull()
                                        if (targetSong != null) {
                                            controls?.moveUpcomingSong(
                                                currentSong, songCount, song, targetSong
                                            )
                                            dragOffsetPx += dragged.offset - target.offset
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedSong = null
                                    dragOffsetPx = 0f
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueActionBar(
    interactive: Boolean,
    onBack: () -> Unit,
    currentSong: Int,
    bottomPadding: Dp,
) {
    val controls = LocalPlayerControls.current
    val repeatMode = controls?.repeatMode ?: RepeatMode.Off
    val shuffle = controls?.shuffleEnabled == true
    val radio = controls?.radioEnabled == true
    val editInteractionSource = remember { MutableInteractionSource() }
    val backInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).padding(bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isEditMode = remember { mutableStateOf(false) }
        FilledIconToggleButton(
            checked = isEditMode.value,
            onCheckedChange = {
                isEditMode.value = it
            },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = colorScheme.primary.copy(0.1f),
                contentColor = colorScheme.onPrimaryContainer,
                checkedContainerColor = colorScheme.primary,
                checkedContentColor = colorScheme.onPrimary
            ),
            shape = animatedPolygonIconShape(
                shape = MaterialShapes.Pill,
                pressedShape = MaterialShapes.Square,
                checkedShape = MaterialShapes.Pill,
                checked = isEditMode.value,
                interactionSource = editInteractionSource,
            ),
            interactionSource = editInteractionSource,
        ) {
            PolygonIconRipple(editInteractionSource) {
                Icon(
                    painterResource(Res.drawable.ic_edit),
                    "Edit"
                )
            }
        }

        ButtonGroup(
            overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
            expandedRatio = 0.12f,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            listOf(
                Pair(
                    "Shuffle", if (shuffle) Res.drawable.ic_shuffle_on else Res.drawable.ic_shuffle
                ),
                Pair(
                    "Repeat", when (repeatMode) {
                        RepeatMode.Off -> Res.drawable.ic_repeat
                        RepeatMode.All -> Res.drawable.ic_repeat_on
                        RepeatMode.One -> Res.drawable.ic_repeat_one_on
                    }
                ),
                Pair("Radio", Res.drawable.ic_sensors),
            ).forEachIndexed { index, (label, icon) ->
                val onActivate: () -> Unit = {
                    when (index) {
                        0 -> controls?.setShuffle(!shuffle, currentSong)
                        1 -> controls?.repeatMode = when (repeatMode) {
                            RepeatMode.Off -> RepeatMode.All
                            RepeatMode.All -> RepeatMode.One
                            RepeatMode.One -> RepeatMode.Off
                        }

                        else -> controls?.radioEnabled = !radio
                    }
                }
                customItem({
                    val source = remember { MutableInteractionSource() }
                    val buttonShape = when (index) {
                        0 -> RoundedCornerShape(
                            topStart = 24.dp, bottomStart = 24.dp,
                            topEnd = 8.dp, bottomEnd = 8.dp,
                        )

                        2 -> RoundedCornerShape(
                            topStart = 8.dp, bottomStart = 8.dp,
                            topEnd = 24.dp, bottomEnd = 24.dp,
                        )

                        else -> RoundedCornerShape(8.dp)
                    }
                    Button(
                        onClick = onActivate,
                        enabled = interactive && controls != null,
                        interactionSource = source,
                        shape = buttonShape,
                        modifier = Modifier.weight(1f).height(playerBottomBarControlSize)
                            .animateWidth(source),
                        contentPadding = when (index) {
                            0 -> PaddingValues(start = 4.dp)
                            2 -> PaddingValues(end = 4.dp)
                            else -> PaddingValues(0.dp)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary.copy(0.1f),
                            contentColor = colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(painterResource(icon), contentDescription = label)
                    }
                }, {
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onActivate(); it.dismiss() },
                        enabled = interactive && controls != null,
                    )
                })
            }
        }

        FilledTonalIconButton(
            onClick = onBack,
            enabled = interactive,
            shape = animatedPolygonIconShape(
                shape = clover6Sided(),
                pressedShape = MaterialShapes.Square,
                interactionSource = backInteractionSource,
            ),
            interactionSource = backInteractionSource,
            colors = IconButtonDefaults.iconButtonColors(
                colorScheme.primary.copy(0.1f),
                colorScheme.onPrimaryContainer
            ),
        ) {
            PolygonIconRipple(backInteractionSource) {
                Icon(painterResource(Res.drawable.ic_back), contentDescription = "Close queue")
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    index: Int,
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val coverUrl = LocalPlayerItems.current.getOrNull(index)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled && !playing, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant)
                .brightenedCoverOutline(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_playlist_play),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
            if (coverUrl != null) {
                BetterImage(
                    model = { coverUrl },
                    contentDescription = "Song $index cover art",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
            Text("Song $index", fontWeight = FontWeight.Bold, style = mergedStyle)
            Text(
                if (playing) "Artist $index · Playing now" else "Artist $index",
                color = LocalContentColor.current.copy(0.66f),
                fontWeight = FontWeight.Normal,
                style = mergedStyle
            )
        }
        IconButton(
            onClick = {},
            Modifier
                .width(32.dp).height(48.dp)
                .semantics { contentDescription = "Hold to drag Song $index" }
                .pointerInput(index, enabled) {
                    if (enabled) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.y)
                            },
                        )
                    }
                },
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_drag_handle),
                contentDescription = null,
            )
        }
    }
}
