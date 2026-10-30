package dev.brahmkshatriya.echo.app.playback

import androidx.compose.runtime.Immutable
import dev.brahmkshatriya.kmedianotification.MediaSession

enum class MediaKind {
    Audio,
    Video,
}

/** An alternate source supplied by the media provider, rather than a transcoding preset. */
@Immutable
data class MediaQuality(
    val id: String,
    val label: String,
    val uri: String,
    val mimeType: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
) {
    init {
        require(id.isNotBlank() && label.isNotBlank() && uri.isNotBlank())
        require(bitrate == null || bitrate > 0)
        require(sampleRate == null || sampleRate > 0)
    }

    val format: String
        get() =
            when (mimeType?.substringBefore(';')?.lowercase()) {
                "audio/mpeg" -> "MP3"
                "audio/mp4",
                "audio/aac" -> "AAC"
                "audio/flac" -> "FLAC"
                "audio/ogg" -> "OGG"
                "audio/wav",
                "audio/x-wav" -> "WAV"
                else ->
                    uri.substringBefore('?')
                        .substringBefore('#')
                        .substringAfterLast('/')
                        .substringAfterLast('.', "")
                        .takeIf { it.length in 1..6 }
                        ?.uppercase() ?: "Audio"
            }
}

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
    /** Additional encodings of the same recording. The original [uri] is always available. */
    val qualities: List<MediaQuality> = emptyList(),
) {
    init {
        require(qualities.none { it.id == "original" }) { "The original quality is implicit" }
        require(qualities.map { it.id }.distinct().size == qualities.size) {
            "Quality IDs must be unique"
        }
    }

    val availableQualities: List<MediaQuality>
        get() = listOf(MediaQuality("original", "Original", uri, mimeType)) + qualities
}

@Immutable data class QueueEntry(val id: Int, val media: PlayableMedia)

@Immutable data class QueueSeed(val id: String, val title: String, val items: List<PlayableMedia>)

enum class RepeatMode {
    Off,
    All,
    One;

    fun next(): RepeatMode =
        when (this) {
            Off -> All
            All -> One
            One -> Off
        }
}

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

enum class SelectionReason {
    Manual,
    Automatic,
}

data class PlayerSelection(val entryId: Int, val reason: SelectionReason, val revision: Long)

data class PlaybackError(val id: Long, val title: String, val details: String)

data class SystemMediaState(
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

interface SystemMediaSession : AutoCloseable {
    val notification: MediaSession

    fun activate()

    fun update(state: SystemMediaState)
}
