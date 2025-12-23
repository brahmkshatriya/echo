package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import dev.brahmkshatriya.echo.extension.spotify.models.Album as AlbumItem

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("__typename")
sealed interface Item {
    @SerialName("__typename")
    val typename: String

    @Serializable
    @SerialName("BrowseSectionContainer")
    data class BrowseSectionContainer(
        @SerialName("__typename")
        override val typename: String,
        val data: CardData? = null,
    ) : Item

    @Suppress("unused")
    @Serializable
    @SerialName("BrowseClientFeature")
    data class BrowseClientFeature(
        @SerialName("__typename")
        override val typename: String,

        val artwork: Artwork? = null,
        val backgroundColor: BackgroundColor? = null,
        val featureUri: String? = null,
        val iconOverlay: Artwork? = null,
    ) : Item

    @Serializable
    @SerialName("PseudoPlaylist")
    data class PseudoPlaylist(
        @SerialName("__typename")
        override val typename: String,

        val name: String? = null,
        val uri: String? = null,
        val image: Artwork? = null,
        val count: Long? = null,
    ) : Item

    @Serializable
    @SerialName("Playlist")
    data class Playlist(
        @SerialName("__typename")
        override val typename: String,

        val content: Content? = null,
        val attributes: List<Attribute>? = null,
        val description: String? = null,
        val format: String? = null,
        val images: Images? = null,
        val name: String? = null,
        val ownerV2: OwnerWrapper? = null,
        val revisionId: String? = null,
        val uri: String? = null,
    ) : Item {
        @Serializable
        data class Content(
            val items: List<Item>? = null,
            val pagingInfo: PagingInfo? = null,
            val totalCount: Long? = null,
        )

        @Serializable
        data class Item(
            val itemV2: TrackWrapper? = null,
            val addedAt: Date? = null,
            val addedBy: OwnerWrapper? = null,
            val attributes: List<Attribute>? = null,
            val uid: String? = null,
        )
    }

    @Serializable
    @SerialName("Album")
    data class Album(
        @SerialName("__typename")
        override val typename: String,
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
    ) : Item, IAlbum

    @Serializable
    @SerialName("PreRelease")
    data class PreRelease(
        @SerialName("__typename")
        override val typename: String,

        val preReleaseContent: Content? = null,
        val preSaved: Boolean? = null,
        val releaseDate: Date? = null,
        val timezone: String? = null,
        val uri: String? = null,
    ) : Item {
        @Serializable
        data class Content(
            val artists: Artists? = null,
            val coverArt: Artwork? = null,
            val name: String? = null,
            val type: String? = null,
            val uri: String? = null,
        )
    }

    @Serializable
    @SerialName("Artist")
    data class Artist(
        @SerialName("__typename")
        override val typename: String,

        override val profile: Profile? = null,
        override val uri: String? = null,
        override val visuals: Visuals? = null,
        override val discography: Discography? = null,
        override val id: String? = null,
        override val relatedContent: RelatedContent? = null,
        override val relatedVideos: RelatedVideos? = null,
        override val saved: Boolean? = null,
        override val stats: Stats? = null,
    ) : Item, IArtist

    @Serializable
    @SerialName("Episode")
    data class Episode(
        @SerialName("__typename")
        override val typename: String,

        val contentRating: ContentRating? = null,
        val coverArt: Artwork? = null,
        val description: String? = null,
        val duration: Duration? = null,
        val mediaTypes: List<String>? = null,
        val name: String? = null,
        val playability: Playability? = null,
        val playedState: PlayedState? = null,
        val podcastV2: CustomWrapper<Podcast>? = null,
        val releaseDate: Date? = null,
        val restrictions: Restrictions? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("Podcast")
    data class Podcast(
        @SerialName("__typename")
        override val typename: String,

        val coverArt: Artwork? = null,
        val mediaTypes: List<String>? = null,
        val name: String? = null,
        val publisher: Profile? = null,
        val topics: Topics? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("Chapter")
    data class Chapter(
        @SerialName("__typename")
        override val typename: String,
        val audiobookV2: CustomWrapper<Audiobook>? = null,
        val contentRating: ContentRating? = null,
        val coverArt: Artwork? = null,
        val description: String? = null,
        val duration: Duration? = null,
        val name: String? = null,
        val playedState: PlayedState? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("Audiobook")
    data class Audiobook(
        @SerialName("__typename")
        override val typename: String,

        val authors: List<Profile>? = null,
        val coverArt: Artwork? = null,
        val description: String? = null,
        val audiobookDuration: Duration? = null,
        val name: String? = null,
        val publishDate: Date? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("Track")
    data class Track(
        @SerialName("__typename")
        override val typename: String,

        override val albumOfTrack: AlbumItem? = null,
        override val artists: Artists? = null,
        val associations: Associations? = null,
        override val contentRating: ContentRating? = null,
        override val duration: Duration? = null,
        override val trackDuration: Duration? = null,
        val id: String? = null,
        override val name: String? = null,
        override val playability: Playability? = null,
        override val playcount: String? = null,
        override val uri: String? = null,
        override val saved: Boolean? = null,
        override val trackNumber: Long? = null,
        override val firstArtist: Artists? = null,
        override val otherArtists: Artists? = null,
    ) : Item, ITrack

    @Serializable
    @SerialName("User")
    data class User(
        @SerialName("__typename")
        override val typename: String,

        val avatar: Artwork? = null,
        val name: String? = null,
        val displayName: String? = null,
        val id: String? = null,
        val username: String? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("Genre")
    data class Genre(
        @SerialName("__typename")
        override val typename: String,

        val image: Artwork? = null,
        val name: String? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("DiscoveryFeed")
    data class DiscoveryFeed(
        @SerialName("__typename")
        override val typename: String,
        val firstItem: Wrapper?,
        val title: String,
        val uri: String,
    ) : Item

    @Serializable
    @SerialName("Folder")
    data class Folder(
        @SerialName("__typename")
        override val typename: String,

        val name: String? = null,
        val uri: String? = null,
        val playlistCount: Int? = null,
        val folderCount: Int? = null,
    ) : Item

    @Serializable
    @SerialName("Merch")
    data class Merch(
        @SerialName("__typename")
        override val typename: String,

        val image: Artwork? = null,
        val nameV2: String? = null,
        val price: String? = null,
        val uri: String? = null,
    ) : Item

    @Serializable
    @SerialName("NotFound")
    data class NotFound(
        @SerialName("__typename")
        override val typename: String,

        val message: String? = null,
    ) : Item

    @Serializable
    @SerialName("GenericError")
    data class GenericError(
        @SerialName("__typename")
        override val typename: String
    ) : Item

    @Serializable
    @SerialName("RestrictedContent")
    data class RestrictedContent(
        @SerialName("__typename")
        override val typename: String,
    ) : Item

    @Serializable
    @SerialName("BrowseSpacesHub")
    data class BrowseSpacesHub(
        @SerialName("__typename")
        override val typename: String,
    ) : Item

    @Serializable
    data class Wrapper(
        @SerialName("__typename")
        val typename: String? = null,
        val data: Item? = null,
        val uri: String? = null,
    )

    @Serializable
    data class CustomWrapper<T : Item>(
        @SerialName("__typename")
        val typename: String? = null,
        val data: T? = null,
        val uri: String? = null,
    )

    @Serializable
    data class OwnerWrapper(
        @SerialName("__typename")
        val typename: String? = null,
        val data: User? = null
    )

    @Serializable
    data class TrackWrapper(
        @SerialName("__typename")
        val typename: String? = null,
        val data: Track? = null,
        @SerialName("_uri")
        val uri: String? = null
    )
}