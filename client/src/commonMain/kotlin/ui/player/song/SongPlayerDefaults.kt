package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

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


/** A changing viewport clips a measured layer without resizing its descendants. */
internal class PlayerViewportShape(private val heightPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rectangle(Rect(0f, 0f, size.width, heightPx.coerceIn(0f, size.height)))
}

/** Values written during measurement and consumed by layer/placement blocks. */
internal class ExpandedTimelineGeometry {
    var metadataHeightPx = 0
    var expandedHeightPx = 0
    val secondaryHeightPx: Int get() = (expandedHeightPx - metadataHeightPx).coerceAtLeast(0)

    fun visibleHeightPx(collapseProgress: Float): Float =
        expandedHeightPx - secondaryHeightPx * collapseProgress.coerceIn(0f, 1f)
}
