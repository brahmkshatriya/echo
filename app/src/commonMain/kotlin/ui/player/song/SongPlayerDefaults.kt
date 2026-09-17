package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val maxSongCoverSize = 360
const val songCoverHorizontalPadding = 16
const val songCoverVerticalPadding = 16

internal const val SongQueueItemContentType = "player-queue-item"
internal const val SongQueueItemsPerGroup = 11

internal const val LyricsWaitingGapMs = 1_500L
internal const val LyricsWaitingDotsPreRollMs = 3_000L
internal const val LyricsWaitingDotsPostRollMs = 2_000L
internal const val LyricsTransitionDurationMs = 240
internal const val LyricsModeTransitionDurationMs = 320

fun Modifier.songCoverSize(
    maxCoverSize: Dp,
    verticalPadding: Dp = songCoverVerticalPadding.dp,
) = padding(songCoverHorizontalPadding.dp, verticalPadding)
    .widthIn(max = maxCoverSize)
    .height(maxCoverSize)
    .aspectRatio(1f)
    .fillMaxSize()
