package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.Serializable
import java.io.InputStream

@Suppress("unused")
@Serializable
data class Streamable(
    val id: String,
    val quality: Int,
    val type: MediaType,
    val title: String? = null,
    val extra: Map<String, String> = mapOf()
) {
    enum class MediaType { Background, Source, Subtitle }
    enum class SubtitleType { VTT, SRT, ASS }
    enum class SourceType { Progressive, HLS, DASH }

    sealed class Media {
        data class Subtitle(val url: String, val type: SubtitleType) : Media()
        data class Sources(val sources: List<Source>, val merged: Boolean) : Media()

        @Serializable
        data class Background(val request: Request) : Media()
        companion object {
            fun Source.toMedia() = Sources(listOf(this), true)
            fun String.toBackgroundMedia(headers: Map<String, String> = mapOf()) =
                Background(this.toRequest(headers))

            fun String.toSourceMedia(
                headers: Map<String, String> = mapOf(),
                type: SourceType = SourceType.Progressive
            ) = this.toSource(headers, type).toMedia()

            fun String.toSubtitleMedia(type: SubtitleType) = Subtitle(this, type)
        }
    }

    sealed class Decryption {
        data class Widevine(
            val license: Request,
            val isMultiSession: Boolean,
        ) : Decryption()
    }

    sealed class Source {
        abstract val quality: Int
        abstract val title: String?

        data class Http(
            val request: Request,
            val type: SourceType = SourceType.Progressive,
            val decryption: Decryption? = null,
            override val quality: Int = 0,
            override val title: String? = null
        ) : Source()

        data class ByteStream(
            val stream: InputStream,
            val totalBytes: Long,
            override val quality: Int = 0,
            override val title: String? = null
        ) : Source()

        data class Channel(
            val channel: ByteReadChannel,
            val totalBytes: Long,
            override val quality: Int = 0,
            override val title: String? = null
        ) : Source()

        companion object {
            fun String.toSource(
                headers: Map<String, String> = mapOf(),
                type: SourceType = SourceType.Progressive
            ) = Http(this.toRequest(headers), type)
        }
    }

    companion object {
        fun source(
            id: String, quality: Int, title: String? = null, extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Source, title, extra)

        fun background(
            id: String, quality: Int, title: String? = null, extra: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Background, title, extra)

        fun subtitle(
            id: String, title: String? = null, extra: Map<String, String> = mapOf()
        ) = Streamable(id, 0, MediaType.Subtitle, title, extra)
    }
}