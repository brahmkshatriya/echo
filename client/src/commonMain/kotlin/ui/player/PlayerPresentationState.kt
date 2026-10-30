package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class PlayerPresentationMode {
    Cover,
    Lyrics,
    Kinetic,
}

@Stable
class PlayerPresentationState {
    var mode by mutableStateOf(PlayerPresentationMode.Cover)
        private set

    var chromeCollapsed by mutableStateOf(false)
        private set

    private var lyricsSources by mutableStateOf<Map<String, Int?>>(emptyMap())

    fun lyricsSource(mediaId: String?): Int? =
        if (mediaId != null && lyricsSources.containsKey(mediaId)) lyricsSources[mediaId] else 0

    fun selectLyricsSource(mediaId: String, index: Int?) {
        val selections = lyricsSources.toMutableMap()
        selections.remove(mediaId)
        selections[mediaId] = index
        while (selections.size > MAX_LYRICS_SOURCES) selections.remove(selections.keys.first())
        lyricsSources = selections.toMap()
    }

    fun show(mode: PlayerPresentationMode) {
        this.mode = mode
        chromeCollapsed = false
    }

    fun updateChromeCollapsed(collapsed: Boolean) {
        chromeCollapsed = collapsed
    }

    companion object {
        private const val MAX_LYRICS_SOURCES = 32
        val Saver =
            listSaver<PlayerPresentationState, Any>(
                save = { state ->
                    buildList {
                        add(state.mode.name)
                        add(state.chromeCollapsed)
                        state.lyricsSources.forEach { (id, index) ->
                            add(id)
                            add(index ?: -1)
                        }
                    }
                },
                restore = { values ->
                    PlayerPresentationState().apply {
                        mode = PlayerPresentationMode.valueOf(values[0] as String)
                        chromeCollapsed = values[1] as Boolean
                        for (index in 2 until values.size step 2) selectLyricsSource(
                            values[index] as String,
                            (values[index + 1] as Int).takeIf { it >= 0 },
                        )
                    }
                },
            )
    }
}

val LocalPlayerPresentation =
    staticCompositionLocalOf<PlayerPresentationState> {
        error("PlayerBottomSheet must provide player presentation state")
    }

@Composable
fun rememberPlayerPresentationState() =
    rememberSaveable(saver = PlayerPresentationState.Saver) { PlayerPresentationState() }
