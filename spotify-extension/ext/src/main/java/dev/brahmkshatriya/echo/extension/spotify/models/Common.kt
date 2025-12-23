package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class Color(
    val hex: String? = null
)

@Serializable
data class Title(
    val transformedLabel: String? = null
)

@Serializable
data class Artwork(
    val extractedColors: ExtractedColors? = null,
    val sources: List<Source>
)

@Serializable
data class ExtractedColors(
    val colorDark: ColorRaw? = null
)

@Serializable
data class ColorRaw(
    val hex: String? = null,
    val isFallback: Boolean? = null
)

@Serializable
data class Source(
    val height: Long? = null,
    val url: String? = null,
    val width: Long? = null
)

@Serializable
data class Profile(
    val avatar: Artwork? = null,
    val avatarBackgroundColor: Long? = null,
    val name: String? = null,
    val uri: String? = null,
    val username: String? = null,
    val verified: Boolean? = null,
    val biography: Biography? = null,
    val pinnedItem: PinnedItem? = null,
    val playlistsV2: ItemsV2? = null,
)

@Serializable
data class PinnedItem(
    val itemV2: Item.Wrapper? = null,
    val comment: String? = null
)

@Serializable
data class Biography(
    val text: String? = null
)

@Serializable
data class ItemsV2(
    val items: List<Item.Wrapper>? = null,
    val totalCount: Long? = null
)

@Serializable
data class Track(
    override val albumOfTrack: Album? = null,
    override val artists: Artists? = null,
    override val contentRating: ContentRating? = null,
    override val duration: Duration? = null,
    override val trackDuration: Duration? = null,
    override val name: String? = null,
    override val playability: Playability? = null,
    override val playcount: String? = null,
    override val uri: String? = null,
    override val saved: Boolean? = null,
    override val trackNumber: Long? = null,
    override val firstArtist: Artists? = null,
    override val otherArtists: Artists? = null,
) : ITrack

interface ITrack {
    val albumOfTrack: Album?
    val artists: Artists?
    val contentRating: ContentRating?
    val duration: Duration?
    val trackDuration: Duration?
    val name: String?
    val playability: Playability?
    val playcount: String?
    val uri: String?
    val saved: Boolean?
    val trackNumber: Long?
    val firstArtist: Artists?
    val otherArtists: Artists?
}

@Serializable
data class TrackItem(
    val track: Track? = null,
    val uid: String? = null,
)

@Serializable
data class Album(
    override val copyright: Copyright? = null,
    override val courtesyLine: String? = null,
    override val label: String? = null,
    override val saved: Boolean? = null,
    override val tracksV2: TracksV2? = null,
    override val discs: Discs? = null,
    override val artists: Artists? = null,
    override val coverArt: Artwork? = null,
    override val name: String? = null,
    override val uri: String? = null,
    override val date: Date? = null,
    override val playability: Playability? = null,
    override val type: String? = null,
    override val id: String? = null,
    override val moreAlbumsByArtist: Artists? = null,
) : IAlbum

interface IAlbum {
    val courtesyLine: String?
    val id: String?
    val date: Date?
    val name: String?
    val uri: String?
    val coverArt: Artwork?
    val copyright: Copyright?
    val label: String?
    val saved: Boolean?
    val tracksV2: TracksV2?
    val discs: Discs?
    val artists: Artists?
    val playability: Playability?
    val type: String?
    val moreAlbumsByArtist: Artists?
}

@Serializable
data class Artists(
    val items: List<Artist>? = null,
    val totalCount: Long? = null
)

@Serializable
data class Artist(
    override val discography: Discography? = null,
    override val id: String? = null,
    override val profile: Profile? = null,
    override val relatedContent: RelatedContent? = null,
    override val relatedVideos: RelatedVideos? = null,
    override val saved: Boolean? = null,
    override val stats: Stats? = null,
    override val uri: String? = null,
    override val visuals: Visuals? = null,
) : IArtist

interface IArtist {
    val discography: Discography?
    val id: String?
    val profile: Profile?
    val relatedContent: RelatedContent?
    val relatedVideos: RelatedVideos?
    val saved: Boolean?
    val stats: Stats?
    val uri: String?
    val visuals: Visuals?
}

@Serializable
data class RelatedContent(
    val appearsOn: Albums? = null,
    val discoveredOnV2: ItemsV2? = null,
    val featuringV2: ItemsV2? = null,
    val relatedArtists: Artists? = null
)

@Serializable
data class RelatedVideos(
    @SerialName("__typename")
    val typename: String? = null,

    val items: JsonArray? = null,
    val totalCount: Long? = null
)

@Serializable
data class Stats(
    val followers: Long? = null,
    val monthlyListeners: Long? = null,
    val topCities: TopCities? = null,
    val worldRank: Long? = null
)

@Serializable
data class TopCities(
    val items: List<TopCitiesItem>? = null
)

@Serializable
data class TopCitiesItem(
    val city: String? = null,
    val country: String? = null,
    val numberOfListeners: Long? = null,
    val region: String? = null
)

@Serializable
data class Discography(
    val all: Albums? = null,
    val albums: Albums? = null,
    val compilations: Albums? = null,
    val latest: Album? = null,
    val popularReleasesAlbums: Releases? = null,
    val singles: Albums? = null,
    val topTracks: TracksV2? = null
)

@Serializable
data class TracksV2(
    val items: List<TrackItem>? = null,
    val totalCount: Long? = null,
)

@Serializable
data class Albums(
    val items: List<AlbumsItem>? = null,
    val totalCount: Long? = null
)

@Serializable
data class AlbumsItem(
    val releases: Releases? = null
)

@Serializable
data class Releases(
    val items: List<Album>? = null,
    val totalCount: Long? = null
)


@Serializable
data class Associations(
    val associatedVideos: AssociatedVideos? = null
)

@Serializable
data class AssociatedVideos(
    val totalCount: Long? = null
)

@Serializable
data class Attribute(
    val key: String? = null,
    val value: String? = null
)

@Serializable
data class BackgroundColor(
    val hex: String? = null
)

@Serializable
data class ContentRating(
    val label: Label? = null
)

@Suppress("unused")
@Serializable
enum class Label { EXPLICIT, NONE }

@Serializable
data class CardData(
    val cardRepresentation: CardRepresentation? = null
)

@Serializable
data class CardRepresentation(
    val artwork: Artwork? = null,
    val backgroundColor: BackgroundColor? = null,
    val title: Title? = null
)

@Serializable
data class Date(
    val isoString: String? = null,
    val precision: String? = null,
    val year: Int? = null
)

@Serializable
data class Duration(
    val totalMilliseconds: Long? = null
)

@Serializable
data class Images(
    val items: List<Artwork>? = null
)

@Serializable
data class Playability(
    val playable: Boolean? = null,
    val reason: String? = null
)

@Serializable
data class PlayedState(
    val playPositionMilliseconds: Long? = null,
    val state: String? = null
)

@Serializable
data class Restrictions(
    val paywallContent: Boolean? = null
)

@Serializable
data class Topics(
    val items: List<TopicsItem>? = null
)

@Serializable
data class TopicsItem(
    @SerialName("__typename")
    val typename: String? = null,

    val title: String? = null,
    val uri: String? = null
)

@Serializable
data class Visuals(
    val avatarImage: Artwork? = null
)

@Serializable
data class PagingInfo(
    val nextOffset: Long? = null,
    val limit: Long? = null,
    val offset: Long? = null,
)

@Serializable
data class Copyright(
    val items: List<CopyrightItem>? = null,
    val totalCount: Long? = null
)

@Serializable
data class CopyrightItem(
    val text: String? = null,
    val type: String? = null
)

@Serializable
data class Discs(
    val items: List<Item>? = null,
    val totalCount: Long? = null
) {

    @Serializable
    data class Item(
        val number: Long? = null,
        val tracks: Tracks? = null
    )

    @Serializable
    data class Tracks(
        val totalCount: Long? = null
    )
}