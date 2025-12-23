package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Metadata4Track(
    val gid: String? = null,
    val name: String? = null,
    val album: Album? = null,
    val artist: List<Artist>? = null,
    val number: Long? = null,

    @SerialName("disc_number")
    val discNumber: Long? = null,

    val duration: Long? = null,
    val popularity: Long? = null,
    val explicit: Boolean? = null,

    @SerialName("external_id")
    val externalId: List<ExternalID>? = null,

    val file: List<File>? = null,
    val preview: List<File>? = null,
    val alternative: List<Alternative>? = null,

    @SerialName("earliest_live_timestamp")
    val earliestLiveTimestamp: Long? = null,

    @SerialName("has_lyrics")
    val hasLyrics: Boolean? = null,

    val licensor: Licensor? = null,

    @SerialName("language_of_performance")
    val languageOfPerformance: List<String>? = null,

    @SerialName("original_audio")
    val originalAudio: OriginalAudio? = null,

    @SerialName("original_title")
    val originalTitle: String? = null,

    @SerialName("artist_with_role")
    val artistWithRole: List<ArtistWithRole>? = null,

    @SerialName("canonical_uri")
    val canonicalURI: String? = null,

    @SerialName("prerelease_config")
    val prereleaseConfig: Metadata4TrackPrereleaseConfig? = null,

    @SerialName("content_authorization_attributes")
    val contentAuthorizationAttributes: String? = null,

    @SerialName("track_content_rating")
    val trackContentRating: List<TrackContentRating>? = null,

    @SerialName("audio_formats")
    val audioFormats: List<AudioFormat>? = null,

    @SerialName("media_type")
    val mediaType: String? = null,
) {

    @Serializable
    data class Album(
        val gid: String? = null,
        val name: String? = null,
        val artist: List<Artist>? = null,
        val label: String? = null,
        val date: Date? = null,

        @SerialName("cover_group")
        val coverGroup: CoverGroup? = null,

        val licensor: Licensor? = null,

        @SerialName("prerelease_config")
        val prereleaseConfig: AlbumPrereleaseConfig? = null
    )

    @Serializable
    data class Artist(
        val gid: String? = null,
        val name: String? = null
    )

    @Serializable
    data class CoverGroup(
        val image: List<Image>? = null
    )

    @Serializable
    data class Image(
        @SerialName("file_id")
        val fileId: String? = null,

        val size: String? = null,
        val width: Long? = null,
        val height: Long? = null
    )

    @Serializable
    data class Date(
        val year: Int? = null,
        val month: Int? = null,
        val day: Int? = null
    )

    @Serializable
    data class Licensor(
        val uuid: String? = null
    )

    @Serializable
    data class AlbumPrereleaseConfig(
        @SerialName("earliest_reveal_date")
        val earliestRevealDate: Date? = null,

        @SerialName("earliest_coverart_reveal_date")
        val earliestCoverArtRevealDate: Date? = null
    )

    @Serializable
    data class ArtistWithRole(
        @SerialName("artist_gid")
        val artistGid: String? = null,

        @SerialName("artist_name")
        val artistName: String? = null,

        val role: String? = null
    )

    @Serializable
    data class AudioFormat(
        @SerialName("original_audio")
        val originalAudio: OriginalAudio? = null
    )

    @Serializable
    data class OriginalAudio(
        val uuid: String? = null,
        val format: String? = null
    )

    @Serializable
    data class ExternalID(
        val type: String? = null,
        val id: String? = null
    )

    @Serializable
    data class Alternative(
        val gid: String? = null,
        val file: List<File>? = null,
        val preview: List<File>? = null
    )

    @Serializable
    data class File(
        @SerialName("file_id")
        val fileId: String? = null,
        val format: Format? = null
    )

    @Serializable
    data class Metadata4TrackPrereleaseConfig(
        @SerialName("earliest_reveal_date")
        val earliestRevealDate: Date? = null
    )

    @Serializable
    data class TrackContentRating(
        val tag: String? = null,
        val markets: List<String>? = null
    )

    @Suppress("unused")
    @Serializable
    enum class Format(val quality: Int) {
        OGG_VORBIS_320(320),
        OGG_VORBIS_160(160),
        OGG_VORBIS_96(96),
        MP4_256_DUAL(256),
        MP4_128_DUAL(128),
        MP4_256(256),
        MP4_128(128),
        AAC_24(240),
        //Preview
        MP3_96(96)
    }
}

