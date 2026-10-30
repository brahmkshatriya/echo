package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

const val maxSongCoverSize = 360
const val songCoverHorizontalPadding = 16
const val songCoverVerticalPadding = 16

const val SongQueueItemContentType = "player-queue-item"
const val SongQueueItemsPerGroup = 11

const val LyricsWaitingGapMs = 1_500L
const val LyricsWaitingDotsPreRollMs = 3_000L
const val LyricsWaitingDotsPostRollMs = 2_000L
const val LyricsTransitionDurationMs = 240
const val LyricsModeTransitionDurationMs = 320


/** A changing viewport clips a measured layer without resizing its descendants. */
class PlayerViewportShape(private val heightPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rectangle(Rect(0f, 0f, size.width, heightPx.coerceIn(0f, size.height)))
}

/** Measurement changes must invalidate retained layers even when playback is paused. */
class ExpandedTimelineGeometry {
    var metadataHeightPx by mutableIntStateOf(0)
    var expandedHeightPx by mutableIntStateOf(0)
    val secondaryHeightPx: Int get() = (expandedHeightPx - metadataHeightPx).coerceAtLeast(0)

    fun visibleHeightPx(collapseProgress: Float): Float =
        expandedHeightPx - secondaryHeightPx * collapseProgress.coerceIn(0f, 1f)
}
