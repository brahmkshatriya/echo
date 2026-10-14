package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.PolygonIconRipple
import dev.brahmkshatriya.echo.app.ui.components.SwitchingIcon
import dev.brahmkshatriya.echo.app.ui.components.animatedPolygonIconShape
import dev.brahmkshatriya.echo.app.ui.components.clipToMaterialGroupViewport
import dev.brahmkshatriya.echo.app.ui.player.song.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.song.exampleSong
import dev.brahmkshatriya.echo.app.ui.player.song.grayCoverOutline
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
    val pointerY: Float? = null,
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
    var heldOrder by remember { mutableStateOf<List<Int>?>(null) }
    val songsInOrder = heldOrder ?: controls?.playbackOrder ?: (0 until songCount).toList()
    val queueGroups = controls?.queues ?: listOf(PlayerQueue(0, "Sample Playlist", songsInOrder))
    val listState = rememberLazyListState()
    var drag by remember { mutableStateOf<QueueDrag?>(null) }
    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val settleJob = remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val fallbackStride = with(LocalDensity.current) { 75.dp.toPx() }
    val edgeZonePx = with(LocalDensity.current) { 80.dp.toPx() }
    val stickyHeaderPx = with(LocalDensity.current) { 64.dp.toPx() }
    val maxAutoScrollPxPerSecond = with(LocalDensity.current) { 680.dp.toPx() }
    var stridePx by remember { mutableFloatStateOf(fallbackStride) }
    val movementSpring = motionScheme.defaultSpatialSpec<Float>()
    val cornerSpring =
        remember { spring<Dp>(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium) }
    val cardColors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
    LaunchedEffect(drag?.song, drag?.settling, interactive) {
        if (!interactive || drag?.settling != false) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val elapsedSeconds = ((frame - previousFrame) / 1_000_000_000f)
                .coerceIn(0f, 0.05f)
            previousFrame = frame
            val active = drag ?: break
            if (active.settling) break
            val pointerY = active.pointerY ?: continue
            val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
            if (viewportHeight <= 0f) continue
            val upperEdge = stickyHeaderPx + edgeZonePx
            val lowerEdge = viewportHeight - edgeZonePx
            val speedFraction = when {
                pointerY < upperEdge -> -((upperEdge - pointerY) / edgeZonePx).coerceIn(0f, 1f)
                pointerY > lowerEdge -> ((pointerY - lowerEdge) / edgeZonePx).coerceIn(0f, 1f)
                else -> 0f
            }
            if (speedFraction == 0f) continue
            val groupIndex = queueGroups.indexOfFirst { active.song in it.songs }
            if (groupIndex < 0) continue
            val group = queueGroups[groupIndex]
            val headerIndex = 1 + queueGroups.take(groupIndex).sumOf { it.songs.size + 1 }
            val lastSongIndex = headerIndex + group.songs.size
            val layoutInfo = listState.layoutInfo
            val requestedScroll = speedFraction * maxAutoScrollPxPerSecond * elapsedSeconds
            val boundedScroll = if (requestedScroll < 0f) {
                when {
                    listState.firstVisibleItemIndex < headerIndex -> 0f
                    listState.firstVisibleItemIndex == headerIndex -> requestedScroll.coerceAtLeast(
                        -listState.firstVisibleItemScrollOffset.toFloat()
                    )

                    else -> requestedScroll
                }
            } else {
                val lastItem = layoutInfo.visibleItemsInfo.firstOrNull {
                    it.key == "queue-song-${group.songs.last()}"
                }
                when {
                    lastItem != null -> requestedScroll.coerceAtMost(
                        (lastItem.offset + lastItem.size - layoutInfo.viewportEndOffset)
                            .coerceAtLeast(0).toFloat()
                    )

                    layoutInfo.visibleItemsInfo.any { it.index > lastSongIndex } -> 0f
                    else -> requestedScroll
                }
            }
            if (boundedScroll == 0f) continue
            val scrolled = listState.scrollBy(boundedScroll)
            val currentDrag = drag
            if (scrolled != 0f && currentDrag?.song == active.song && !currentDrag.settling) {
                val distance = currentDrag.distance + scrolled
                val queueStart =
                    queueGroups.takeWhile { active.song !in it.songs }.sumOf { it.songs.size }
                val queueEnd =
                    queueStart + (queueGroups.firstOrNull { active.song in it.songs }?.songs?.size
                        ?: 1) - 1
                val destination = (active.from + (distance / stridePx).roundToInt())
                    .coerceIn(queueStart, queueEnd)
                drag = currentDrag.copy(distance = distance, to = destination)
            }
        }
    }
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
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val thumbSpacerHeight = (maxHeight * 0.6f - 64.dp - 68.dp - 37.5.dp).coerceAtLeast(0.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().onGloballyPositioned { listCoordinates = it },
                userScrollEnabled = interactive && drag == null,
            ) {
                item(key = "queue-thumb-spacer", contentType = "queue-spacer") {
                    Spacer(Modifier.fillMaxWidth().height(thumbSpacerHeight))
                }

                queueGroups.forEachIndexed { groupIndex, group ->
                    val groupStart = queueGroups.take(groupIndex).sumOf { it.songs.size }
                    val groupEnd = groupStart + group.songs.size
                    val groupSongs = songsInOrder.subList(groupStart, groupEnd)
                    val groupKeys = groupSongs.mapTo(mutableSetOf<String>()) { "queue-song-$it" }
                    val firstSongKey = groupSongs.firstOrNull()?.let { "queue-song-$it" } ?: ""
                    val lastSongKey = groupSongs.lastOrNull()?.let { "queue-song-$it" } ?: ""
                    stickyHeader(key = "queue-group-${group.id}", contentType = "queue-group") {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(group.title, style = typography.titleMedium)
                            Text(
                                buildString {
                                    append("${group.songs.size} songs")
                                    if (currentSong in group.songs) append(" · Playing")
                                    if (group.shuffleEnabled) append(" · Shuffled")
                                    if (group.repeatMode != RepeatMode.Off) append(" · Repeat ${group.repeatMode.name}")
                                },
                                style = typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    itemsIndexed(
                        items = groupSongs,
                        key = { _, song -> "queue-song-$song" },
                        contentType = { _, _ -> "queue-song" },
                    ) { localIndex, song ->
                        val index = groupStart + localIndex
                        val active = drag
                        val pinnableContainer = LocalPinnableContainer.current
                        DisposableEffect(active?.song == song, pinnableContainer) {
                            val pin = if (active?.song == song) pinnableContainer?.pin() else null
                            onDispose { pin?.release() }
                        }
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
                        val landingOrder =
                            if (active?.settling == true && active.from in groupStart until groupEnd) {
                                songsInOrder.toMutableList().apply {
                                    add(active.to, removeAt(active.from))
                                }
                            } else null
                        val topEdge = (landingOrder ?: songsInOrder).getOrNull(groupStart) == song
                        val bottomEdge =
                            (landingOrder ?: songsInOrder).getOrNull(groupEnd - 1) == song
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
                        val rowTranslationY =
                            if (active?.song == song) active.distance else neighborY
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .zIndex(if (active?.song == song) 1f else 0f)
                                .graphicsLayer {
                                    translationY = rowTranslationY
                                }
                                .padding(bottom = 3.dp)
                                .clipToMaterialGroupViewport(
                                    lazyListState = listState,
                                    itemKey = "queue-song-$song",
                                    itemKeys = groupKeys,
                                    firstKey = firstSongKey,
                                    lastKey = lastSongKey,
                                    radius = 20.dp,
                                    itemTranslationY = rowTranslationY,
                                    clipToGroupBounds = active?.song != song,
                                ),
                            colors = cardColors,
                            shape = RoundedCornerShape(
                                topStart = topCorner, topEnd = topCorner,
                                bottomStart = bottomCorner, bottomEnd = bottomCorner,
                            ),
                        ) {
                            QueueSongRow(
                                index = song,
                                listCoordinates = listCoordinates,
                                playing = song == currentSong,
                                enabled = interactive,
                                onClick = { if (song != currentSong) onSongSelected(song) },
                                onDragStart = {
                                    val previous = drag
                                    if (previous == null || previous.settling) {
                                        val previousOrder = heldOrder ?: songsInOrder
                                        val completingMove =
                                            previous?.settling == true && previous.to != previous.from
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
                                            val visibleOffset =
                                                listState.firstVisibleItemScrollOffset
                                            settleJob.value?.cancel()
                                            settleJob.value = null
                                            Snapshot.withMutableSnapshot {
                                                if (completingMove) controls?.moveQueueSong(
                                                    previous.song,
                                                    previous.to
                                                )
                                                stridePx = item?.size?.toFloat()?.coerceAtLeast(1f)
                                                    ?: fallbackStride
                                                heldOrder = nextOrder
                                                drag = QueueDrag(
                                                    song = song,
                                                    from = nextIndex,
                                                    to = nextIndex
                                                )
                                            }
                                            if (completingMove) {
                                                listState.requestScrollToItem(
                                                    visibleIndex,
                                                    visibleOffset
                                                )
                                            }
                                        }
                                    }
                                },
                                onDrag = { delta, pointerY ->
                                    val gesture = drag
                                    if (gesture?.song == song && !gesture.settling) {
                                        val distance = gesture.distance + delta
                                        val destination =
                                            (gesture.from + (distance / stridePx).roundToInt())
                                                .coerceIn(groupStart, groupEnd - 1)
                                        drag = gesture.copy(
                                            distance = distance,
                                            to = destination,
                                            pointerY = pointerY
                                        )
                                    }
                                },
                                onDragEnd = {
                                    val gesture = drag
                                    if (gesture?.song == song && !gesture.settling) {
                                        if (gesture.distance == 0f && gesture.to == gesture.from) {
                                            drag = null
                                            heldOrder = null
                                        } else {
                                            drag = gesture.copy(settling = true)
                                            settleJob.value = scope.launch {
                                                val destination =
                                                    (gesture.to - gesture.from) * stridePx
                                                Animatable(gesture.distance).animateTo(
                                                    destination,
                                                    movementSpring
                                                ) {
                                                    if (drag?.song == song && drag?.settling == true) {
                                                        drag = drag?.copy(distance = value)
                                                    }
                                                }
                                                if (drag?.song == song && drag?.settling == true) {
                                                    val moved = gesture.to != gesture.from
                                                    val visibleIndex =
                                                        listState.firstVisibleItemIndex
                                                    val scrollOffset =
                                                        listState.firstVisibleItemScrollOffset
                                                    Snapshot.withMutableSnapshot {
                                                        if (moved) controls?.moveQueueSong(
                                                            song,
                                                            gesture.to
                                                        )
                                                        drag = null
                                                        heldOrder = null
                                                    }
                                                    if (moved) listState.requestScrollToItem(
                                                        visibleIndex,
                                                        scrollOffset
                                                    )
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
                colorScheme.secondaryContainer,
                colorScheme.onSecondaryContainer,
                checkedContainerColor = colorScheme.secondary,
                checkedContentColor = colorScheme.onSecondary,
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
                            containerColor = colorScheme.primaryContainer,
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
                colorScheme.secondary,
                colorScheme.onSecondary
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
    listCoordinates: LayoutCoordinates?,
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Float, Float?) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
) {
    val coverUrl = LocalPlayerItems.current.getOrNull(index)
    val song = exampleSong(index)
    val coverModel = remember(coverUrl) { { coverUrl } }
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    val latestListCoordinates by rememberUpdatedState(listCoordinates)
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
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
                .grayCoverOutline(
                    outlineColor = Color.Gray,
                    outlineRadius = 4.dp,
                ),
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
                    contentDescription = "${song?.album ?: "Album"} cover art",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
            Text(
                song?.title ?: "Unknown track", fontWeight = FontWeight.Bold, style = mergedStyle,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                (song?.artist ?: "Unknown artist") + if (playing) " · Playing now" else "",
                color = LocalContentColor.current.copy(0.66f),
                fontWeight = FontWeight.Normal,
                style = mergedStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .height(48.dp).width(32.dp)
                .semantics { contentDescription = "Drag to reorder ${song?.title ?: "track"}" }
                .onGloballyPositioned { handleCoordinates = it }
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
                                val list = latestListCoordinates
                                val handle = handleCoordinates
                                val pointerY =
                                    if (list?.isAttached == true && handle?.isAttached == true) {
                                        list.localPositionOf(handle, change.position).y
                                    } else null
                                latestDrag(amount.y, pointerY)
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_drag_indicator),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(0.5f)
            )
        }
    }
}
