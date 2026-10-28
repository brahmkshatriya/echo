package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.ui.errors.playerTickerAnchor
import dev.brahmkshatriya.echo.app.playback.*
import dev.brahmkshatriya.echo.app.ui.player.artwork.*
import dev.brahmkshatriya.echo.app.ui.player.components.*
import dev.brahmkshatriya.echo.app.ui.player.song.SongPlayerItem
import dev.brahmkshatriya.kmplayer.compose.KMVideoSurface

@Composable
internal fun MediaPlayerPage(
    i: Int,
    onScrolledToTopChanged: (Boolean) -> Unit,
    onInteractionLockChanged: (Boolean) -> Unit,
) {
    val session = LocalPlayerControls.current
    val media = session.mediaFor(i) ?: return
    PlayerArtworkHost(i, MaxPlayerArtworkSize.dp) {
        CompositionLocalProvider(LocalPlayerTimelineState provides session.timelineFor(i), LocalPlayerEntryId provides i) {
            PrecomposedPlayerContent(
                compactContent = { CollapsedPlayer(i, showCover = true) },
                expandedContent = {
                    when (media.kind) {
                        MediaKind.Audio -> SongPlayerItem(i, onScrolledToTopChanged,
                            onLyricsSelectorOpenChanged = onInteractionLockChanged)
                        MediaKind.Video -> VideoPlayerItem(i, onInteractionLockChanged)
                    }
                },
            )
        }
    }
}

@Composable
private fun VideoPlayerItem(entryId: Int, onInteractionLockChanged: (Boolean) -> Unit) {
    val session = LocalPlayerControls.current
    val sheet = LocalPlayerSheet.current
    val transition = LocalPlayerQueueTransition.current
    val media = session.mediaFor(entryId) ?: return
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var queueButton by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val active = session.currentEntryId == entryId
    LaunchedEffect(active, transition.open, transition.mounted) {
        if (active) onInteractionLockChanged(transition.open || transition.mounted)
    }
    SideEffect {
        if (active) { transition.alternateSource = null; transition.onAlternateClick = null }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { root = it }) {
    Column(Modifier.fillMaxSize().graphicsLayer {
        alpha = (sheet?.progressState?.value ?: 1f).coerceIn(0f, 1f)
    }.padding(WindowInsets.safeDrawing.asPaddingValues())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(media.title, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (media.artist.isNotEmpty()) Text(media.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LikeToggle(entryId)
            TextButton(onClick = { sheet?.collapse() }) { Text("Collapse") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Adjacent retained pages never attach the active player's surface.
            if (session.currentEntryId == entryId) {
                session.nativePlayer?.let { KMVideoSurface(it, Modifier.fillMaxSize()) }
            }
        }
        TimelineWithVolume(Modifier.fillMaxWidth())
        Controller()
        Row(Modifier.fillMaxWidth().playerTickerAnchor(entryId).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RepeatButton()
            Spacer(Modifier.weight(1f))
            MoreButton()
            QueueButton(
                onClick = {
                    val parent = root
                    val button = queueButton
                    if (parent != null && button != null && parent.isAttached && button.isAttached)
                        transition.openFrom(parent.localBoundingBoxOf(button, clipBounds = false))
                },
                modifier = Modifier.onGloballyPositioned { button ->
                    queueButton = button
                    val parent = root
                    if (active && parent != null && parent.isAttached && button.isAttached)
                        transition.updateAnchor(parent.localBoundingBoxOf(button, clipBounds = false))
                },
            )
        }
    }
    CollapsedPlayer(entryId, showCover = true)
    }
}
