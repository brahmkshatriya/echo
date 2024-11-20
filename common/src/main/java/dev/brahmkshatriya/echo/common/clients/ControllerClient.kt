package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface ControllerClient : ExtensionClient {
    /**
     * Whether the the extension can perform actions when the player is paused.
     * Only set this to false if you are sure that the extension does not need to perform any actions when the player is paused.
     */
    var runsDuringPause: Boolean

    // app -> controller
    /**
     * Called when the playback state changes.
     * @param isPlaying Whether the player is playing.
     * @param position The current position of the track.
     * @param track The current track in the player.
     */
    suspend fun onPlaybackStateChanged(isPlaying: Boolean, position: Double, track: Track)
    /**
     * Called when the playlist changes.
     * @param playlist The new playlist.
     * The first track is not necessarily the current track.
     */
    suspend fun onPlaylistChanged(playlist: List<Track>)
    /**
     * Called when the playback mode changes.
     * @param isShuffle Whether shuffle mode is enabled.
     * @param repeatState The repeat mode.
     * @see androidx.media3.common.Player.REPEAT_MODE_OFF
     */
    suspend fun onPlaybackModeChanged(isShuffle: Boolean, repeatState: Int)
    /**
     * Called when the position of the track changes.
     * @param position The new position of the track.
     */
    suspend fun onPositionChanged(position: Double)
    /**
     * Called when the volume of the player changes.
     * @param volume The new volume of the player.
     */
    suspend fun onVolumeChanged(volume: Double)

    // controller -> app
    /**
     * Called when the controller requests to play the player.
     */
    var onPlayRequest: (suspend () -> Unit)?
    /**
     * Called when the controller requests to pause the player.
     */
    var onPauseRequest: (suspend () -> Unit)?
    /**
     * Called when the controller requests to play the next track.
     */
    var onNextRequest: (suspend () -> Unit)?
    /**
     * Called when the controller requests to play the previous track.
     */
    var onPreviousRequest: (suspend () -> Unit)?
    /**
     * Called when the controller requests to seek to a position in the track.
     * @param position The position to seek to.
     */
    var onSeekRequest: (suspend (position: Double) -> Unit)?
    /**
     * Called when the controller requests to seek to a track in the playlist.
     * @param index The index of the track to seek to.
     */
    var onSeekToMediaItemRequest: (suspend (index: Int) -> Unit)?
    /**
     * Called when the controller requests to move a track in the playlist.
     * @param fromIndex The index of the track to move.
     * @param toIndex The index to move the track to.
     */
    var onMovePlaylistItemRequest: (suspend (fromIndex: Int, toIndex: Int) -> Unit)?
    /**
     * Called when the controller requests to remove a track from the playlist.
     * @param index The index of the track to remove.
     */
    var onRemovePlaylistItemRequest: (suspend (index: Int) -> Unit)?
    /**
     * Called when the controller requests to enable or disable shuffle mode.
     * @param enabled Whether shuffle mode should be enabled.
     */
    var onShuffleModeRequest: (suspend (enabled: Boolean) -> Unit)?
    /**
     * Called when the controller requests to change the repeat mode.
     * @param repeatMode The new repeat mode.
     * @see androidx.media3.common.Player.REPEAT_MODE_OFF
     */
    var onRepeatModeRequest: (suspend (repeatMode: Int) -> Unit)?
    /**
     * Called when the controller requests to change the volume of the player.
     * @param volume The new volume of the player.
     */
    var onVolumeRequest: (suspend (volume: Double) -> Unit)?
}