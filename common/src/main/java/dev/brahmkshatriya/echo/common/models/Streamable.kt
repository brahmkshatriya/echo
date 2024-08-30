package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.serialization.Serializable
import java.io.InputStream

@Suppress("unused")
@Serializable
data class Streamable(
    val id: String,
    val quality: Int,
    val mediaType: MediaType,
    val mimeType: MimeType = MimeType.Progressive,
    val title: String? = null,
    val extra: Map<String, String> = mapOf()
) {
    enum class MimeType { Progressive, HLS, DASH }
    enum class MediaType { Audio, Video, AudioVideo }

    sealed class Media {
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
            ) : WithVideo()
        }

        companion object {
            fun Audio.toMedia() = AudioOnly(this)
            fun String.toVideoMedia(headers: Map<String, String> = mapOf()) =
                WithVideo.Only(this.toRequest(headers))

            fun String.toAudioVideoMedia(headers: Map<String, String> = mapOf()) =
                WithVideo.WithAudio(this.toRequest(headers))
        }
    }

    sealed class Audio {
        abstract val skipSilence: Boolean?

        data class Http(
            val request: Request,
            override val skipSilence: Boolean? = null
        ) : Audio()

        data class ByteStream(
            val stream: InputStream, val totalBytes: Long,
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
            type : MimeType = MimeType.Progressive,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Audio, type, title, extra)

        fun video(
            id: String,
            quality: Int,
            type : MimeType = MimeType.Progressive,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Video, type, title, extra)

        fun audioVideo(
            id: String,
            quality: Int,
            type : MimeType = MimeType.Progressive,
            title: String? = null,
            extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.AudioVideo, type, title, extra)
    }
}