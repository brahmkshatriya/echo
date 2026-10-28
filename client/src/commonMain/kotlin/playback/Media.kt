package dev.brahmkshatriya.echo.app.playback

import androidx.compose.runtime.Immutable
import dev.brahmkshatriya.kmedianotification.MediaSession

enum class MediaKind { Audio, Video }

/** Media identity is independent of its position or number of occurrences in a queue. */
@Immutable
data class PlayableMedia(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val artworkUri: String? = null,
    val durationMs: Long? = null,
    val kind: MediaKind = MediaKind.Audio,
    val mimeType: String? = null,
)

@Immutable
data class QueueEntry(val id: Int, val media: PlayableMedia)

@Immutable
data class QueueSeed(val id: String, val title: String, val items: List<PlayableMedia>)

enum class RepeatMode { Off, All, One }

@Immutable
data class PlayerQueue(
    val id: String,
    val title: String,
    val entries: List<QueueEntry>,
    val unshuffledOrder: List<Int> = entries.map { it.id },
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
) {
    val entryIds = entries.map { it.id }
}

internal enum class SelectionReason { Manual, Automatic }
internal data class PlayerSelection(val entryId: Int, val reason: SelectionReason, val revision: Long)

data class PlaybackError(val id: Long, val title: String, val details: String)

internal data class SystemMediaState(
    val media: PlayableMedia?,
    val playing: Boolean,
    val loading: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val canGoNext: Boolean,
    val canGoPrevious: Boolean,
    val speed: Double,
    val volume: Float = 1f,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val entryId: Int? = null,
    val queue: List<QueueEntry> = emptyList(),
    val favourite: Boolean = false,
)

internal interface SystemMediaSession : AutoCloseable {
    val notification: MediaSession
    fun activate()
    fun update(state: SystemMediaState)
}
