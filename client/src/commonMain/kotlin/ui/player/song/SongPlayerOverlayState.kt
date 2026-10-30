package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import dev.brahmkshatriya.echo.app.ui.player.components.PlayerPillTransition

/** Page-owned geometry; lyrics and queue are mutually exclusive overlays. */
internal class SongPlayerOverlayState(
    private val lyrics: PlayerPillTransition,
    private val queue: PlayerPillTransition,
) {
    var playerCoordinates by mutableStateOf<LayoutCoordinates?>(null)
        private set

    var lyricsPillCoordinates by mutableStateOf<LayoutCoordinates?>(null)
        private set

    var queueButtonCoordinates by mutableStateOf<LayoutCoordinates?>(null)
        private set

    fun updatePlayer(coordinates: LayoutCoordinates) {
        playerCoordinates = coordinates
    }

    fun updateLyricsPill(coordinates: LayoutCoordinates) {
        lyricsPillCoordinates = coordinates
    }

    fun updateQueueButton(coordinates: LayoutCoordinates) {
        queueButtonCoordinates = coordinates
    }

    fun localBounds(anchor: LayoutCoordinates?): Rect? {
        val root = playerCoordinates
        if (root?.isAttached != true || anchor?.isAttached != true) return null
        return root.localBoundingBoxOf(anchor, clipBounds = false)
    }

    fun queueBounds(
        density: Density,
        container: DpSize,
        anchor: LayoutCoordinates? = queueButtonCoordinates,
    ): Rect? {
        val bounds = localBounds(anchor) ?: return null
        val width = with(density) { container.width.toPx() }
        val height = with(density) { container.height.toPx() }
        return Rect(
            bounds.center.x - width / 2f,
            bounds.center.y - height / 2f,
            bounds.center.x + width / 2f,
            bounds.center.y + height / 2f,
        )
    }

    fun openLyrics(bounds: Rect) {
        queue.close()
        lyrics.openFrom(bounds)
    }

    fun openQueue(bounds: Rect) {
        lyrics.close()
        queue.openFrom(bounds)
    }
}
