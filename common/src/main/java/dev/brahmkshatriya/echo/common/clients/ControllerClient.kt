package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.serialization.Serializable

interface ControllerClient : ExtensionClient {
    @Serializable
    enum class RepeatMode {
        OFF,
        ONE,
        ALL
    }

    /**
     * The current state of the player.
     * @param isPlaying Whether the player is playing.
     * @param currentTrack The current track in the player.
     * @param currentPosition The current position of the track.
     * @param playlist The current playlist.
     * @param currentIndex The index of the current track in the playlist. -1 if the playlist is empty.
     * @param shuffle Whether shuffle mode is enabled.
     * @param repeatMode The repeat mode.
     * @see RepeatMode
     * @param volume The volume of the player normalized between 0 and 1.
     */
    @Serializable
    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentTrack: Track? = null,
        val currentPosition: Long = 0,
        val playlist: List<Track> = emptyList(),
        val currentIndex: Int = 0,
        val shuffle: Boolean = false,
        val repeatMode: RepeatMode = RepeatMode.OFF,
        val volume: Double = 0.0
    )

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
    suspend fun onPlaybackStateChanged(isPlaying: Boolean, position: Long, track: Track?)

    /**
     * Called when the playlist changes.
     * @param playlist The new playlist.
     * The first track is not necessarily the current track.
     */
    suspend fun onPlaylistChanged(playlist: List<Track>)

    /**
     * Called when the playback mode changes.
     * @param isShuffle Whether shuffle mode is enabled.
     * @param repeatMode The repeat mode.
     * @see RepeatMode
     */
    suspend fun onPlaybackModeChanged(isShuffle: Boolean, repeatMode: RepeatMode)

    /**
     * Called when the position of the track changes.
     * @param position The new position of the track.
     */
    suspend fun onPositionChanged(position: Long)

    /**
     * Called when the volume of the player changes.
     * @param volume The new volume of the player.
     */
    suspend fun onVolumeChanged(volume: Double)

    // controller -> app
    /**
     * Called when the controller requests the current state of the player.
     * @return The current state of the player.
     */
    var onRequestState: (suspend () -> PlayerState)?

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
     * passes the position in milliseconds.
     */
    var onSeekRequest: (suspend (position: Long) -> Unit)?

    /**
     * Called when the controller requests to seek to a track in the playlist.
     * passes the index of the track.
     */
    var onSeekToMediaItemRequest: (suspend (index: Int) -> Unit)?

    /**
     * Called when the controller requests to move a track in the playlist.
     * passes the index of the track to move and the new index.
     */
    var onMovePlaylistItemRequest: (suspend (fromIndex: Int, toIndex: Int) -> Unit)?

    /**
     * Called when the controller requests to remove a track from the playlist.
     * passes the index of the track to remove.
     */
    var onRemovePlaylistItemRequest: (suspend (index: Int) -> Unit)?

    /**
     * Called when the controller requests to enable or disable shuffle mode.
     * passes whether shuffle mode should be enabled.
     */
    var onShuffleModeRequest: (suspend (enabled: Boolean) -> Unit)?

    /**
     * Called when the controller requests to change the repeat mode.
     * passes the new repeat mode.
     * @see RepeatMode
     */
    var onRepeatModeRequest: (suspend (repeatMode: RepeatMode) -> Unit)?

    /**
     * Called when the controller requests to change the volume of the player.
     * passes the new volume of the player.
     */
    var onVolumeRequest: (suspend (volume: Double) -> Unit)?
}