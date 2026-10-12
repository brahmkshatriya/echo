package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.PolygonIconRipple
import dev.brahmkshatriya.echo.app.ui.components.SwitchingIcon
import dev.brahmkshatriya.echo.app.ui.components.animatedPolygonIconShape
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_back
import echo.client.generated.resources.ic_drag_indicator
import echo.client.generated.resources.ic_edit
import echo.client.generated.resources.ic_playlist_play
import echo.client.generated.resources.ic_repeat
import echo.client.generated.resources.ic_repeat_on
import echo.client.generated.resources.ic_repeat_one_on
import echo.client.generated.resources.ic_shuffle
import echo.client.generated.resources.ic_shuffle_on
import echo.client.generated.resources.ic_timer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private data class QueueDrag(
    val song: Int,
    val from: Int,
    val to: Int,
    val distance: Float = 0f,
    val settling: Boolean = false,
)

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
    // Physical item order remains unchanged until the gesture finishes. Songs
    // retain their keys (and their loaded artwork) throughout the reorder.
    var heldOrder by remember { mutableStateOf<List<Int>?>(null) }
    val songsInOrder = heldOrder ?: controls?.playbackOrder ?: (0 until songCount).toList()
    val listState = rememberLazyListState()
    var drag by remember { mutableStateOf<QueueDrag?>(null) }
    val settleJob = remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val fallbackStride = with(LocalDensity.current) { 75.dp.toPx() }
    var stridePx by remember { mutableFloatStateOf(fallbackStride) }
    val movementSpring = motionScheme.defaultSpatialSpec<Float>()
    val cornerSpring = remember { spring<Dp>(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium) }
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
            userScrollEnabled = interactive && drag == null,
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
            itemsIndexed(
                items = songsInOrder,
                key = { _, song -> "queue-song-$song" },
                contentType = { _, _ -> "queue-song" },
            ) { index, song ->
                val active = drag
                val neighborOffset = when {
                    active == null || song == active.song -> 0f
                    active.to > active.from && index in (active.from + 1)..active.to -> -stridePx
                    active.to < active.from && index in active.to until active.from -> stridePx
                    else -> 0f
                }
                val neighborY by key(active?.song) {
                    animateFloatAsState(
                        targetValue = neighborOffset,
                        animationSpec = movementSpring,
                        label = "Queue neighbor",
                    )
                }
                val landingOrder = if (active?.settling == true) {
                    songsInOrder.toMutableList().apply {
                        add(active.to, removeAt(active.from))
                    }
                } else null
                val topEdge = (landingOrder ?: songsInOrder).firstOrNull() == song
                val bottomEdge = (landingOrder ?: songsInOrder).lastOrNull() == song
                val topCorner by animateDpAsState(
                    targetValue = if ((active == null || active.settling) && topEdge) 20.dp else 6.dp,
                    animationSpec = cornerSpring,
                    label = "Queue top corner",
                )
                val bottomCorner by animateDpAsState(
                    targetValue = if ((active == null || active.settling) && bottomEdge) 20.dp else 6.dp,
                    animationSpec = cornerSpring,
                    label = "Queue bottom corner",
                )
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .zIndex(if (active?.song == song) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (active?.song == song) active.distance else neighborY
                        }
                        .padding(bottom = 3.dp),
                    colors = cardColors,
                    shape = RoundedCornerShape(
                        topStart = topCorner, topEnd = topCorner,
                        bottomStart = bottomCorner, bottomEnd = bottomCorner,
                    ),
                ) {
                    QueueSongRow(
                        index = song,
                        playing = song == currentSong,
                        enabled = interactive,
                        onClick = { if (song != currentSong) onSongSelected(song) },
                        onDragStart = {
                            val previous = drag
                            if (previous == null || previous.settling) {
                                val previousOrder = heldOrder ?: songsInOrder
                                val completingMove = previous?.settling == true && previous.to != previous.from
                                val nextOrder = if (completingMove) {
                                    previousOrder.toMutableList().apply {
                                        add(previous.to, removeAt(previous.from))
                                    }
                                } else previousOrder
                                val nextIndex = nextOrder.indexOf(song)
                                val item = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == "queue-song-$song" }
                                if (nextIndex >= 0) {
                                    val visibleIndex = listState.firstVisibleItemIndex
                                    val visibleOffset = listState.firstVisibleItemScrollOffset
                                    settleJob.value?.cancel()
                                    settleJob.value = null
                                    Snapshot.withMutableSnapshot {
                                        if (completingMove) controls?.moveQueueSong(previous.song, previous.to)
                                        stridePx = item?.size?.toFloat()?.coerceAtLeast(1f) ?: fallbackStride
                                        heldOrder = nextOrder
                                        drag = QueueDrag(song = song, from = nextIndex, to = nextIndex)
                                    }
                                    if (completingMove) {
                                        listState.requestScrollToItem(visibleIndex, visibleOffset)
                                    }
                                }
                            }
                        },
                        onDrag = { delta ->
                            val gesture = drag
                            if (gesture?.song == song && !gesture.settling) {
                                val distance = gesture.distance + delta
                                val destination = (gesture.from + (distance / stridePx).roundToInt())
                                    .coerceIn(0, songsInOrder.lastIndex)
                                drag = gesture.copy(distance = distance, to = destination)
                            }
                        },
                        onDragEnd = {
                            val gesture = drag
                            if (gesture?.song == song && !gesture.settling) {
                                if (gesture.distance == 0f && gesture.to == gesture.from) {
                                    // A tap has nothing to settle.
                                    drag = null
                                    heldOrder = null
                                } else {
                                    drag = gesture.copy(settling = true)
                                    settleJob.value = scope.launch {
                                        // The same settle animation handles both moves to
                                        // a new index and returns to the original index.
                                        // Do not clear the drag until this spring completes.
                                        val destination = (gesture.to - gesture.from) * stridePx
                                        Animatable(gesture.distance).animateTo(destination, movementSpring) {
                                            if (drag?.song == song && drag?.settling == true) {
                                                drag = drag?.copy(distance = value)
                                            }
                                        }
                                        if (drag?.song == song && drag?.settling == true) {
                                            val moved = gesture.to != gesture.from
                                            val visibleIndex = listState.firstVisibleItemIndex
                                            val scrollOffset = listState.firstVisibleItemScrollOffset
                                            Snapshot.withMutableSnapshot {
                                                if (moved) controls?.moveQueueSong(song, gesture.to)
                                                drag = null
                                                heldOrder = null
                                            }
                                            if (moved) listState.requestScrollToItem(visibleIndex, scrollOffset)
                                        }
                                    }
                                }
                            }
                        },
                    )
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
                containerColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
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
                Pair("Timer", Res.drawable.ic_timer),
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
                        SwitchingIcon(icon = icon, contentDescription = label)
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
                shape = MaterialShapes.Circle,
                pressedShape = MaterialShapes.Square,
                interactionSource = backInteractionSource,
            ),
            interactionSource = backInteractionSource,
            colors = IconButtonDefaults.iconButtonColors(
                colorScheme.tertiary,
                colorScheme.onTertiary
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
    val coverModel = remember(coverUrl) { { coverUrl } }
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled && !playing, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorScheme.surfaceVariant)
                .brightenedCoverOutline(outlineRadius = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_playlist_play),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
            if (coverUrl != null) {
                BetterImage(
                    model = coverModel,
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
        Box(
            modifier = Modifier
                .height(48.dp).width(32.dp)
                .semantics { contentDescription = "Drag to reorder Song $index" }
                .pointerInput(index, enabled) {
                    if (enabled) {
                        detectDragGestures(
                            orientationLock = null,
                            shouldAwaitTouchSlop = { false },
                            onDragStart = { _, _, _ -> latestDragStart() },
                            onDragEnd = { latestDragEnd() },
                            onDragCancel = { latestDragEnd() },
                            onDrag = { change, amount ->
                                change.consume()
                                latestDrag(amount.y)
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_drag_indicator),
                contentDescription = null,
                tint = colorScheme.primary.copy(0.5f)
            )
        }
    }
}
