package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface ControllerClient : ExtensionClient {
    // app -> controller
    suspend fun onPlaybackStateChanged(isPlaying: Boolean, position: Double, track: Track)
    suspend fun onPlaylistChanged(playlist: List<Track>)
    suspend fun onPlaybackModeChanged(isShuffle: Boolean, repeatState: Int)
    suspend fun onPositionChanged(position: Double)
    suspend fun onVolumeChanged(volume: Double)

    // controller -> app
    var onPlayRequest: (suspend () -> Unit)?
    var onPauseRequest: (suspend () -> Unit)?
    var onNextRequest: (suspend () -> Unit)?
    var onPreviousRequest: (suspend () -> Unit)?
    var onSeekRequest: (suspend (position: Double) -> Unit)?
    var onMovePlaylistItemRequest: (suspend (fromIndex: Int, toIndex: Int) -> Unit)?
    var onRemovePlaylistItemRequest: (suspend (index: Int) -> Unit)?
    var onShuffleModeRequest: (suspend (enabled: Boolean) -> Unit)?
    var onRepeatModeRequest: (suspend (repeatMode: Int) -> Unit)?
    var onVolumeRequest: (suspend (volume: Double) -> Unit)?
}