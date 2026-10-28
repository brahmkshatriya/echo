package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class PlayerPresentationMode { Cover, Lyrics, Kinetic }

@Stable
class PlayerPresentationState {
    var mode by mutableStateOf(PlayerPresentationMode.Cover)
    var chromeCollapsed by mutableStateOf(false)
    private val lyricsSources = mutableStateMapOf<String, Int?>()
    fun lyricsSource(mediaId: String?): Int? = if (mediaId != null && lyricsSources.containsKey(mediaId))
        lyricsSources[mediaId] else 0
    fun selectLyricsSource(mediaId: String, index: Int?) { lyricsSources[mediaId] = index }
    fun show(mode: PlayerPresentationMode) { this.mode = mode; chromeCollapsed = false }

    companion object {
        val Saver = listSaver<PlayerPresentationState, Any>(
            save = { state -> buildList {
                add(state.mode.name); add(state.chromeCollapsed)
                state.lyricsSources.forEach { (id, index) -> add(id); add(index ?: -1) }
            } },
            restore = { values -> PlayerPresentationState().apply {
                mode = PlayerPresentationMode.valueOf(values[0] as String)
                chromeCollapsed = values[1] as Boolean
                for (index in 2 until values.size step 2) {
                    lyricsSources[values[index] as String] = (values[index + 1] as Int).takeIf { it >= 0 }
                }
            } },
        )
    }
}

val LocalPlayerPresentation = staticCompositionLocalOf<PlayerPresentationState> {
    error("PlayerBottomSheet must provide player presentation state")
}

@Composable
internal fun rememberPlayerPresentationState() = rememberSaveable(saver = PlayerPresentationState.Saver) {
    PlayerPresentationState()
}
