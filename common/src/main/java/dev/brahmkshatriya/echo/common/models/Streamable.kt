package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Streamable.Decryption.Widevine
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Background
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toBackgroundMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toSubtitleMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Server
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Subtitle
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Http
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Raw
import kotlinx.serialization.Serializable
import java.io.InputStream

/**
 * A data class representing an unloaded streamable item that is used when playing a [Track]
 * The streamable item can be of three types:
 * - [Streamable.server] - To represent a server that contains data to be played
 * - [Streamable.background] - To represent a background video
 * - [Streamable.subtitle] - To represent subtitle
 *
 * See [TrackClient.loadStreamableMedia] for loading this streamable item.
 *
 * @property id The id of the streamable item
 * @property quality The quality of the streamable item, this is used to sort the streamable items
 * @property type The type of the streamable item
 * @property title The title of the streamable item
 * @property extras Any extra data you want to associate with the streamable item
 *
 * @see Streamable.Media
 */
@Suppress("unused")
@Serializable
data class Streamable(
    val id: String,
    val quality: Int,
    val type: MediaType,
    val title: String? = null,
    val extras: Map<String, String> = mapOf()
) {

    /**
     * A class that represents a loaded streamable media.
     *
     * There are three types of media:
     * - [Subtitle] - To represent a loaded subtitle media
     * - [Server] - To represent a loaded server media
     * - [Background] - To represent a loaded background media
     *
     * @see toMedia
     * @see toSubtitleMedia
     * @see toServerMedia
     * @see toBackgroundMedia
     */
    sealed class Media {

        /**
         * A data class representing a loaded subtitle for a [Track].
         *
         * Headers are unfortunately not supported for subtitles.
         *
         * @property url The url of the subtitle
         * @property type The type of the subtitle
         *
         * @see SubtitleType
         * @see toSubtitleMedia
         */
        data class Subtitle(val url: String, val type: SubtitleType) : Media()

        /**
         * A data class representing a loaded server media for a [Track].
         *
         * The sources will all load at the same time if [merged] is true and combined into a
         * single media source like a M3U8 with multiple qualities.
         *
         * If [merged] is false, the sources will be loaded separately and
         * the user can switch between them.
         *
         * @property sources The list of sources for the server media
         * @property merged Whether the server media is merged or not
         *
         * @see Source
         * @see Source.toMedia
         * @see toServerMedia
         */
        data class Server(val sources: List<Source>, val merged: Boolean) : Media()

        /**
         * A data class representing a loaded background video for a [Track].
         * The sound of the background video will be removed.
         *
         * @property request The request for the background media
         *
         * @see Request
         * @see toBackgroundMedia
         */
        @Serializable
        data class Background(val request: Request) : Media()
        companion object {
            /**
             * Creates a [Server] media from this [Source].
             */
            fun Source.toMedia() = Server(listOf(this), false)

            /**
             * Creates a [Background] media from this String Url.
             *
             * @param headers The headers to be used for the request
             * @return A [Background] media from the String Url
             */
            fun String.toBackgroundMedia(headers: Map<String, String> = mapOf()) =
                Background(this.toRequest(headers))

            /**
             * Creates a single [Source] server media from this String Url.
             *
             * @param headers The headers to be used for the request
             * @param type The type of the source
             * @return A single [Source.Http] media with the given [type]
             */
            fun String.toServerMedia(
                headers: Map<String, String> = mapOf(),
                type: SourceType = SourceType.Progressive,
                isVideo: Boolean = false
            ) = this.toSource(headers, type, isVideo).toMedia()

            /**
             * Creates a [Subtitle] media from this String Url.
             *
             * @param type The type of the subtitle
             * @return A [Subtitle] media from the String Url
             */
            fun String.toSubtitleMedia(type: SubtitleType) = Subtitle(this, type)
        }
    }

    /**
     * A class representing Media Decryption for a [Source].
     *
     * There is only one type of decryption:
     * - [Widevine] - To represent a Widevine decryption
     */
    sealed class Decryption {

        /**
         * A data class representing a Widevine decryption for a [Source].
         *
         * @property license The license request for the Widevine decryption
         * @property isMultiSession Whether the Widevine decryption is multi-session or not
         *
         * @see Request
         */
        data class Widevine(
            val license: Request,
            val isMultiSession: Boolean,
        ) : Decryption()
    }

    /**
     * A class representing the actual source where streamable audio/video is present.
     *
     * There are three types of sources:
     * - [Http] - To represent a source that contains Audio/Video on a Http Url.
     * - [Raw] - To represent a source that contains Audio/Video in a Byte Stream.
     *
     * @property quality The quality of the source, this is used to sort the sources
     * @property title The title of the source
     *
     * @see SourceType
     * @see toSource
     * @see Media.toServerMedia
     */
    sealed class Source {
        abstract val quality: Int
        abstract val title: String?
        open val isVideo: Boolean = false

        /**
         * A data class representing a source that contains Audio/Video on a Http Url.
         *
         * @property request The request for the source
         * @property type The type of the source
         * @property decryption The decryption for the source
         *
         * @see Request
         * @see Decryption
         */
        data class Http(
            val request: Request,
            val type: SourceType = SourceType.Progressive,
            val decryption: Decryption? = null,
            override val quality: Int = 0,
            override val title: String? = null,
            override val isVideo: Boolean = false
        ) : Source()

        /**
         * A data class representing a source that contains Audio/Video in a Byte Stream.
         *
         * @property streamProvider A function that provides an [InputStream] from a given position.
         *
         * @see InputProvider
         */
        data class Raw(
            val streamProvider: InputProvider,
            override val quality: Int = 0,
            override val title: String? = null,
            override val isVideo: Boolean = false
        ) : Source()

        companion object {
            /**
             * Creates a [Http] source from the String Url.
             *
             * @param headers The headers to be used for the request
             * @param type The type of the source
             * @return A [Source] from the String Url
             */
            fun String.toSource(
                headers: Map<String, String> = mapOf(),
                type: SourceType = SourceType.Progressive,
                isVideo: Boolean = false
            ) = Http(this.toRequest(headers), type, isVideo = isVideo)

            fun InputProvider.toSource(isVideo: Boolean = false) = Raw(this, isVideo = isVideo)
        }
    }

    /**
     * An interface that provides an [InputStream] from a given position.
     *
     * This is used for [Streamable.Source.Raw] to provide the stream data.
     */
    fun interface InputProvider {

        /**
         * Provides an [InputStream] from a given position.
         *
         * @param position The position to start reading from, 0 if the stream should start from the beginning
         * @param length The total bytes that should be the end of the stream, -1 if unknown. Important for seeking.
         * @return An [InputStream] from the given position and the total bytes that can be read, or -1 if unknown.
         */
        suspend fun provide(position: Long, length: Long): Pair<InputStream, Long>
    }

    companion object {

        /**
         * Creates a [Streamable] with the given [id], [quality], [title], and [extras] for a server media.
         *
         * @param id The id of the streamable item
         * @param quality The quality of the streamable item, this is used to sort the streamable items
         * @param title The title of the streamable item
         * @param extras Any extra data you want to associate with the streamable item
         * @return A [Streamable] with the given [id], [quality], [title], and [extras] for a server media
         */
        fun server(
            id: String, quality: Int, title: String? = null, extras: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Server, title, extras)

        /**
         * Creates a [Streamable] with the given [id], [quality], [title], and [extras] for a background media.
         *
         * @param id The id of the streamable item
         * @param quality The quality of the streamable item, this is used to sort the streamable items
         * @param title The title of the streamable item
         * @param extras Any extra data you want to associate with the streamable item
         * @return A [Streamable] with the given [id], [quality], [title], and [extras] for a background media
         */
        fun background(
            id: String, quality: Int, title: String? = null, extras: Map<String, String> = mapOf()
        ) = Streamable(id, quality, MediaType.Background, title, extras)

        /**
         * Creates a [Streamable] with the given [id], [quality], [title], and [extras] for a subtitle media.
         *
         * @param id The id of the streamable item
         * @param title The title of the streamable item
         * @param extras Any extra data you want to associate with the streamable item
         * @return A [Streamable] with the given [id], [title], and [extras] for a subtitle media
         */
        fun subtitle(
            id: String, title: String? = null, extras: Map<String, String> = mapOf()
        ) = Streamable(id, 0, MediaType.Subtitle, title, extras)
    }

    /**
     * An enum class representing the type of media
     */
    enum class MediaType {
        /** Represents an unloaded background streamable */
        Background,

        /** Represents an unloaded server streamable */
        Server,

        /** Represents an unloaded subtitle streamable */
        Subtitle
    }

    /**
     * An enum class representing the type of subtitle
     */
    enum class SubtitleType {
        /** WebVTT subtitle format */
        VTT,

        /** SubRip subtitle format */
        SRT,

        /** Advanced SubStation Alpha subtitle format */
        ASS
    }

    /**
     * An enum representing the type of [Source].
     */
    enum class SourceType {
        /**
         * Source that contain Audio/Video in container format File.
         */
        Progressive,

        /**
         * Source that is a M3U8 File.
         */
        HLS,

        /**
         * Source that is a Dash Manifest File.
         */
        DASH
    }
}