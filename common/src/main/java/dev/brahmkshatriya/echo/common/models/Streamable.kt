package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.Serializable
import java.io.InputStream

@Suppress("unused")
@Serializable
data class Streamable(
    val id: String,
    val quality: Int,
    val mediaType: MediaType,
    val mimeType: MimeType = MimeType.Progressive,
    val decryptionType: DecryptionType? = null,
    val title: String? = null,
    val extra: Map<String, String> = mapOf()
) {
    enum class MimeType { Progressive, HLS, DASH }
    enum class MediaType { Audio, Video, AudioVideo, Subtitle }
    enum class SubtitleType { VTT, SRT, ASS }

    @Serializable
    sealed class DecryptionType {
        @Serializable
        data class Widevine(val license: Request, val isMultiSession: Boolean) : DecryptionType()
    }

    sealed class Media {
        data class Subtitle(val url: String, val type: SubtitleType) : Media()

        data class AudioOnly(val audio: Audio) : Media()

        @Serializable
        sealed class WithVideo : Media() {
            abstract val request: Request
            abstract val crop: Boolean

            @Serializable
            data class Only(
                override val request: Request,
                override val crop: Boolean = false,
                val looping: Boolean = false,
            ) : WithVideo()

            @Serializable
            data class WithAudio(
                override val request: Request,
                override val crop: Boolean = false,
                val skipSilence: Boolean? = null,
            ) : WithVideo() {
                fun toAudio() = Audio.Http(request, skipSilence)
            }
        }

        companion object {
            fun Audio.toMedia() = AudioOnly(this)
            fun String.toVideoMedia(headers: Map<String, String> = mapOf()) =
                WithVideo.Only(this.toRequest(headers))

            fun String.toAudioVideoMedia(headers: Map<String, String> = mapOf()) =
                WithVideo.WithAudio(this.toRequest(headers))

            fun String.toSubtitleMedia(type: SubtitleType) = Subtitle(this, type)
        }
    }

    sealed class Audio {
        abstract val skipSilence: Boolean?

        open class Http(
            open val request: Request,
            override val skipSilence: Boolean? = null
        ) : Audio()

        data class ByteStream(
            val stream: InputStream,
            val totalBytes: Long,
            override val skipSilence: Boolean? = null
        ) : Audio()

        data class Channel(
            val channel: ByteReadChannel,
            val totalBytes: Long,
            override val skipSilence: Boolean? = null
        ) : Audio()

        companion object {
            fun String.toAudio(headers: Map<String, String> = mapOf()) =
                Http(this.toRequest(headers))
        }
    }

    companion object {
        fun audio(
            id: String,
            quality: Int,
            type: MimeType = MimeType.Progressive,
            decryptionType: DecryptionType? = null,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Audio, type, decryptionType, title, extra)

        fun video(
            id: String,
            quality: Int,
            type: MimeType = MimeType.Progressive,
            decryptionType: DecryptionType? = null,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Video, type, decryptionType, title, extra)

        fun audioVideo(
            id: String,
            quality: Int,
            type: MimeType = MimeType.Progressive,
            decryptionType: DecryptionType? = null,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.AudioVideo, type, decryptionType, title, extra)

        fun subtitle(
            id: String,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(
            id, 0, MediaType.Subtitle, MimeType.Progressive, null, title, extra
        )
    }
}