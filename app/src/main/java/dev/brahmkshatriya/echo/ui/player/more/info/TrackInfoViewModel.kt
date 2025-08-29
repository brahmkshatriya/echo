package dev.brahmkshatriya.echo.ui.player.more.info

import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.extensions.cache.Cached.loadMedia
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.media.MediaDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class TrackInfoViewModel(
    val app: App,
    extensionLoader: ExtensionLoader,
    playerState: PlayerState,
    downloader: Downloader
) : MediaDetailsViewModel(
    downloader, app, true,
    playerState.current.combine(extensionLoader.music) { current, music ->
        current?.mediaItem?.extensionId?.let { id ->
            music.find { it.id == id }
        }
    }
) {
    val currentFlow = playerState.current
    override fun getItem(): Triple<String, EchoMediaItem, Boolean>? {
        return currentFlow.value?.let { (_, item) ->
            Triple(item.extensionId, item.track, item.isLoaded)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            listOf(playerState.current, extensionFlow, refreshFlow).merge().collectLatest {
                itemResultFlow.value = null
                val extension = extensionFlow.value ?: return@collectLatest
                val track = currentFlow.value?.mediaItem?.takeIf { it.isLoaded }?.track
                    ?: return@collectLatest
                itemResultFlow.value = loadMedia(
                    app,
                    extension,
                    MediaState.Unloaded(extension.id, track)
                )
            }
        }
    }
}