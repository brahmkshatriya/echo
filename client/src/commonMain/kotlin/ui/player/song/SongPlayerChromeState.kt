package dev.brahmkshatriya.echo.app.ui.player.song


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import dev.brahmkshatriya.echo.app.ui.player.PlayerPresentationMode
import dev.brahmkshatriya.echo.app.ui.player.PlayerPresentationState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Only the current page may update the shared chrome; outgoing pages retain their UI. */
class SongPlayerChromeState(
    private val presentation: PlayerPresentationState,
    private val isCurrentItem: () -> Boolean,
) {
    var interactionNonce by mutableIntStateOf(0)
        private set

    fun setCollapsed(collapsed: Boolean) {
        if (isCurrentItem()) presentation.updateChromeCollapsed(collapsed)
    }

    fun interact() {
        if (!isCurrentItem() || presentation.mode == PlayerPresentationMode.Cover) return
        setCollapsed(false)
        restartTimeout()
    }

    fun restartTimeout() {
        interactionNonce++
    }

    fun reset() {
        interactionNonce = 0
        setCollapsed(false)
    }
}

@Composable
fun rememberSongPlayerChromeState(
    entryId: Int,
    presentation: PlayerPresentationState,
    isCurrentItem: Boolean,
    isSeeking: Boolean,
    isBottomBarSticky: Boolean,
): SongPlayerChromeState {
    val currentItem = rememberUpdatedState(isCurrentItem)
    val chrome = remember(entryId, presentation) {
        SongPlayerChromeState(presentation) { currentItem.value }
    }
    val immersiveMode = presentation.mode != PlayerPresentationMode.Cover
    LaunchedEffect(chrome, immersiveMode) { if (!immersiveMode) chrome.reset() }
    LaunchedEffect(chrome, isSeeking, immersiveMode, isCurrentItem) {
        if (immersiveMode && isSeeking) chrome.interact()
    }
    LaunchedEffect(
        chrome,
        presentation.mode,
        chrome.interactionNonce,
        isSeeking,
        isCurrentItem,
        isBottomBarSticky,
    ) {
        if (!isCurrentItem || !immersiveMode || isSeeking || isBottomBarSticky)
            return@LaunchedEffect
        delay(3_000.milliseconds)
        chrome.setCollapsed(true)
    }
    return chrome
}
