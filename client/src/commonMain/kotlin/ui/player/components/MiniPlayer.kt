package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.hasTouchInput
import dev.brahmkshatriya.echo.app.playback.PlayerSession
import dev.brahmkshatriya.echo.app.ui.components.ResponsiveRow
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPadding
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.artwork.PlayerArtwork
import dev.brahmkshatriya.echo.app.ui.player.collapsedHorizontalPadding
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_close
import echo.client.generated.resources.ic_keyboard_arrow_down
import org.jetbrains.compose.resources.painterResource

@Composable
fun StickyMiniPlayer(
    session: PlayerSession,
    index: Int,
) {
    val playerSheet = LocalPlayerSheet.current

    CollapsedPlayerContent(
        session,
        index,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            PlayerArtwork(
                contentDescription = "${session.mediaFor(index)?.album ?: "Album"} cover art",
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.primaryFixed),
            )
        },
        trailingContent = {
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = { playerSheet?.collapse() },
                interactionSource = interactionSource,
                shapes = IconButtonDefaults.shapes(),
                colors = defaultIconButtonColors(),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = "Collapse Player"
                )
            }
        },
    )
}

@Composable
fun CollapsedPlayer(
    session: PlayerSession,
    i: Int,
    showCover: Boolean = false,
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    CollapsedPlayerContent(
        session,
        i,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.ModulateAlpha
                val sheetProgress = playerSheet?.progressState?.value ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                alpha = 1 - positiveProgress
                translationY = -positiveProgress * size.height
            }
            .padding(playerPadding)
            .padding(top = 8.dp, end = 2.dp)
            .padding(horizontal = collapsedHorizontalPadding.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { playerSheet?.expand() }
            .padding(8.dp),
        leadingContent = {
            Box(Modifier.size(48.dp)) {
                AnimatedVisibility(
                    visible = showCover,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(250)),
                ) {
                    PlayerArtwork(
                        contentDescription = "${session.mediaFor(i)?.album ?: "Album"} artwork",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.primaryFixed),
                    )
                }
            }
        },
        trailingContent = {
            if (!hasTouchInput.value) {
                val interactionSource = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { playerSheet?.hide() },
                    interactionSource = interactionSource,
                    modifier = Modifier.size(44.dp),
                    shapes = IconButtonDefaults.shapes(),
                    colors = defaultIconButtonColors()
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_close),
                        contentDescription = "Close Player"
                    )
                }
            }
        },
    )
}

@Composable
private fun CollapsedPlayerContent(
    session: PlayerSession,
    i: Int,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    val navigator = dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSkipNavigator.current
    val timelineState = session.mediaFor(i)?.let { session.timelineFor(i) }
    val song = session.mediaFor(i)
    ResponsiveRow(
        modifier = modifier,
        spacing = 4.dp,
    ) {
        item(priority = 90, key = "artwork") {
            leadingContent()
        }

        item(priority = 80, key = "metadata") {
            Column(Modifier.width(128.dp).padding(start = 4.dp)) {
                val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
                Text(
                    song?.title ?: "Unknown track",
                    fontWeight = FontWeight.Bold,
                    style = mergedStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song?.artist ?: "Unknown artist",
                    fontWeight = FontWeight.Normal,
                    style = mergedStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        spacer(
            whenItemHidden = "timeline-volume",
            key = "metadata-favourite-spacer",
            weight = 1f,
        )

        item(priority = 60, key = "favourite") {
            LikeToggle(session.isFavourite(i)) { session.setFavourite(i, it) }
        }

        item(priority = 10, key = "timeline-volume", weight = 1f) {
            timelineState?.let { timeline ->
                TimelineWithVolume(
                    timelineState = timeline,
                    isPlaying = session.isPlaying,
                    volume = session.volume,
                    lastAudibleVolume = session.lastAudibleVolume,
                    onVolumeChange = session::setVolume,
                    onSeekPreview = { session.previewSeek(i, it) },
                    onSeekCommit = { session.commitSeek(i) },
                    onSeekCancel = { session.cancelScrub(i) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(priority = 49, key = "previous") {
            PreviousButton(
                enabled = timelineState != null,
                onClick = { if (navigator != null) navigator.previous() else session.previous() },
                modifier = Modifier.height(44.dp).width(36.dp),
            )
        }

        item(
            priority = 110,
            key = "play-pause-compact",
            whenItemHidden = "timeline-volume",
        ) {
            CompactPlayPauseButton(
                isPlaying = session.isPlaying,
                isLoading = session.isLoading,
                timelineState = timelineState,
                onClick = session::togglePlayPause,
            )
        }

        item(
            priority = 110,
            key = "play-pause",
            whenItemVisible = "timeline-volume",
        ) {
            PlayPauseButton(session.isPlaying, session.isLoading, session::togglePlayPause, Modifier.size(44.dp))
        }

        item(priority = 50, key = "next") {
            NextButton(
                enabled = timelineState != null && navigator?.canGoNext == true,
                onClick = { navigator?.next() },
                modifier = Modifier.height(44.dp).width(36.dp),
            )
        }

        item(priority = 30, key = "repeat") {
            RepeatButton(session.repeatMode, { session.repeatMode = session.repeatMode.next() }, Modifier.size(44.dp))
        }

        item(priority = 100, key = "trailing") {
            trailingContent()
        }
    }
}
