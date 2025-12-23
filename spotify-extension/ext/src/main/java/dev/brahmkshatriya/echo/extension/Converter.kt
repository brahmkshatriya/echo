package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.spotify.Base62
import dev.brahmkshatriya.echo.extension.spotify.Queries
import dev.brahmkshatriya.echo.extension.spotify.models.Albums
import dev.brahmkshatriya.echo.extension.spotify.models.Artists
import dev.brahmkshatriya.echo.extension.spotify.models.Artwork
import dev.brahmkshatriya.echo.extension.spotify.models.Canvas
import dev.brahmkshatriya.echo.extension.spotify.models.HomeFeed
import dev.brahmkshatriya.echo.extension.spotify.models.IAlbum
import dev.brahmkshatriya.echo.extension.spotify.models.IArtist
import dev.brahmkshatriya.echo.extension.spotify.models.ITrack
import dev.brahmkshatriya.echo.extension.spotify.models.Item
import dev.brahmkshatriya.echo.extension.spotify.models.Item.Wrapper
import dev.brahmkshatriya.echo.extension.spotify.models.ItemsV2
import dev.brahmkshatriya.echo.extension.spotify.models.Label
import dev.brahmkshatriya.echo.extension.spotify.models.LibraryV3
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track
import dev.brahmkshatriya.echo.extension.spotify.models.ProfileAttributes
import dev.brahmkshatriya.echo.extension.spotify.models.Releases
import dev.brahmkshatriya.echo.extension.spotify.models.SearchDesktop
import dev.brahmkshatriya.echo.extension.spotify.models.Sections
import dev.brahmkshatriya.echo.extension.spotify.models.Sections.ItemsItem
import dev.brahmkshatriya.echo.extension.spotify.models.Sections.SectionItem
import dev.brahmkshatriya.echo.extension.spotify.models.TracksV2
import dev.brahmkshatriya.echo.extension.spotify.models.UserFollowers
import dev.brahmkshatriya.echo.extension.spotify.models.UserProfileView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil


fun List<HomeFeed.Chip>.toTabs() = map {
    Tab(it.id!!, it.label?.transformedLabel!!)
}

fun HomeFeed.Home.toShelves(queries: Queries, cropCovers: Boolean): List<Shelf> {
    return sectionContainer?.sections?.toShelves(
        queries, cropCovers, greeting?.transformedLabel ?: "What's on your mind?"
    )!!
}

fun Sections.toShelves(
    queries: Queries,
    cropCovers: Boolean,
    emptyTitle: String? = null,
    token: String? = null,
): List<Shelf> {
    return items?.mapNotNull { item ->
        item.data ?: return@mapNotNull null
        if (item.data.typename == Sections.Typename.BrowseRelatedSectionData)
            return@mapNotNull item.toCategory(queries, cropCovers)

        val uri = item.uri ?: return@mapNotNull null
        val title = item.data.title?.transformedLabel ?: emptyTitle ?: ""
        val subtitle = item.data.subtitle?.transformedLabel
        when (item.data.typename) {
            null -> null
            Sections.Typename.BrowseGenericSectionData ->
                Shelf.Lists.Items(
                    id = uri,
                    title = title,
                    subtitle = subtitle,
                    list = item.sectionItems?.items?.mapNotNull { it.content.toMediaItem(cropCovers) }!!,
                )

            Sections.Typename.HomeGenericSectionData, Sections.Typename.HomeSpotlightSectionData ->
                Shelf.Lists.Items(
                    id = uri,
                    title = title,
                    subtitle = subtitle,
                    list = item.sectionItems?.items?.mapNotNull { it.content.toMediaItem(cropCovers) }!!,
                    more = if ((item.sectionItems.totalCount ?: 0) > 3) paged<Shelf> { offset ->
                        val sectionItem = queries.homeSection(uri, token, offset).json
                            .data.homeSections.sections.first().sectionItems
                        val next = sectionItem.pagingInfo?.nextOffset
                        sectionItem.items!!.mapNotNull {
                            it.content.toMediaItem(cropCovers)?.toShelf()
                        } to next
                    }.toFeed() else null
                )

            Sections.Typename.BrowseGridSectionData -> {
                Shelf.Lists.Categories(
                    id = uri,
                    title = title,
                    subtitle = subtitle,
                    list = item.sectionItems?.items?.mapNotNull {
                        it.toBrowseCategory(queries, cropCovers)
                    }!!,
                    type = Shelf.Lists.Type.Grid
                )
            }

            Sections.Typename.BrowseRelatedSectionData -> throw IllegalStateException()
            Sections.Typename.HomeShortsSectionData -> Shelf.Lists.Items(
                id = uri,
                title = title,
                subtitle = subtitle,
                list = item.sectionItems?.items?.mapNotNull { it.content.toMediaItem(cropCovers) }!!,
                type = Shelf.Lists.Type.Grid
            )

            Sections.Typename.HomeFeedBaselineSectionData -> item.sectionItems?.items
                ?.firstOrNull()?.content?.data?.toMediaItem(cropCovers)?.toShelf()

            Sections.Typename.BrowseUnsupportedSectionData -> null
            Sections.Typename.HomeOnboardingSectionDataV2 -> null
            Sections.Typename.HomeWatchFeedSectionData -> null
            Sections.Typename.HomeRecentlyPlayedSectionData -> null
        }
    }!!
}

private fun SectionItem.toCategory(api: Queries, cropCovers: Boolean): Shelf.Category? {
    val item = sectionItems?.items?.firstOrNull() ?: return null
    return item.toBrowseCategory(api, cropCovers)
}

fun ITrack.toTrack(
    cropCovers: Boolean,
    a: Album? = null,
    url: String? = null,
    added: Date? = null,
): Track? {
    val album = albumOfTrack?.toAlbum(cropCovers, name) ?: a
    return Track(
        id = uri ?: url ?: return null,
        title = name ?: return null,
        cover = album?.cover,
        artists = artists.toArtists(null, cropCovers),
        album = album,
        isExplicit = contentRating?.label == Label.EXPLICIT,
        duration = duration?.totalMilliseconds ?: trackDuration?.totalMilliseconds,
        plays = playcount?.toLong(),
        releaseDate = album?.releaseDate,
        playlistAddedDate = added
    )
}

fun Item.Playlist.toPlaylist(cropCovers: Boolean): Playlist? {
    val desc = description?.removeHtml()?.ifEmpty { null }
    return Playlist(
        id = uri ?: return null,
        title = name ?: return null,
        isEditable = false,
        description = desc,
        subtitle = ownerV2?.data?.name,
        cover = images?.items?.firstOrNull()?.toImageHolder(cropCovers),
        authors = listOfNotNull(ownerV2?.data?.toMediaItem(cropCovers) as? Artist),
        trackCount = content?.totalCount,
        duration = content?.toDuration(),
        creationDate = content?.items?.map { it.addedAt?.toDate() }?.maxByOrNull { it ?: Date(0) }
    )
}

private fun Item.Playlist.Content.toDuration(): Long? {
    val items = items ?: return null
    val average = items.run {
        sumOf { it.itemV2?.data?.trackDuration?.totalMilliseconds ?: 0 }.toDouble() / items.size
    }
    val count = totalCount ?: return null
    return (average * count).toLong()
}

fun Item.PseudoPlaylist.toPlaylist(cropCovers: Boolean): Playlist? {
    return Playlist(
        id = uri ?: return null,
        title = name ?: return null,
        isEditable = true,
        cover = image?.toImageHolder(cropCovers),
        trackCount = count
    )
}

fun Item.Playlist.toRadio(cropCovers: Boolean): Radio? {
    return Radio(
        id = uri ?: return null,
        title = name ?: return null,
        subtitle = description?.removeHtml(),
        cover = images?.items?.firstOrNull()?.toImageHolder(cropCovers)
    )
}

fun IAlbum.toAlbum(cropCovers: Boolean, n: String? = null): Album? {
    return Album(
        id = uri ?: return null,
        title = name ?: n ?: return null,
        subtitle = date?.year?.toString(),
        artists = artists.toArtists(null, cropCovers),
        cover = coverArt?.toImageHolder(cropCovers),
        trackCount = tracksV2?.totalCount,
        duration = tracksV2?.toDuration(),
        releaseDate = date?.toDate(),
        description = buildString {
            if (!courtesyLine.isNullOrBlank()) appendLine(courtesyLine)
            copyright?.items?.forEach {
                appendLine(it.text)
            }
        },
        label = label
    )
}

fun TracksV2.toDuration(): Long? {
    val average = items?.run {
        sumOf { it.track?.duration?.totalMilliseconds ?: 0 }.toDouble() / items.size
    } ?: return null
    val count = totalCount ?: return null
    return (average * count).toLong()
}

fun dev.brahmkshatriya.echo.extension.spotify.models.Date.toDate(): Date? {
    if (isoString != null) return isoString.toDate(precision)
    if (year != null) return Date(year)
    return null
}

private fun String.toDate(precision: String? = null): Date {
    val locale = Locale.ENGLISH
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    val targetTime = runCatching { formatter.parse(this) }.getOrElse {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", locale).run {
            timeZone = TimeZone.getTimeZone("UTC")
            parse(this@toDate)
        }
    }
    val calendar = Calendar.getInstance()
    calendar.time = targetTime
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    if (precision == "YEAR") return Date(year)
    if (precision == "MONTH") return Date(year, month)
    return Date(year, month, day)
}

fun IArtist.toArtist(type: String? = null, cropCovers: Boolean): Artist? {
    return Artist(
        id = uri ?: return null,
        name = profile?.name ?: return null,
        cover = visuals?.avatarImage?.toImageHolder(cropCovers),
        subtitle = type,
        bio = profile?.biography?.text?.removeHtml()
    )
}

fun Artists?.toArtists(subtitle: String? = null, cropCovers: Boolean) =
    this?.items?.mapNotNull { it.toArtist(subtitle, cropCovers) } ?: listOf()

fun Item.Podcast.toAlbum(cropCovers: Boolean): Album? {
    return Album(
        id = uri ?: return null,
        title = name ?: return null,
        type = Album.Type.Show,
        subtitle = "Podcast",
        cover = coverArt?.toImageHolder(cropCovers),
    )
}

fun Item.Audiobook.toAlbum(cropCovers: Boolean): Album? {
    return Album(
        id = uri ?: return null,
        title = name ?: return null,
        type = Album.Type.Book,
        cover = coverArt?.toImageHolder(cropCovers),
        description = description?.removeHtml(),
        subtitle = "Audiobook",
        releaseDate = publishDate?.toDate(),
    )
}

fun Artists.toShelf(
    id: String, title: String, subtitle: String? = null, more: Feed<Shelf>, cropCovers: Boolean,
): Shelf? {
    if (items.isNullOrEmpty()) return null
    return Shelf.Lists.Items(
        id = id,
        title = title,
        subtitle = subtitle,
        list = items.mapNotNull { it.toArtist(subtitle, cropCovers) },
        more = if (items.size > 3) more else null
    )
}

fun Albums.toShelf(id: String, title: String, more: Feed<Shelf>, cropCovers: Boolean): Shelf? {
    if (items.isNullOrEmpty()) return null
    return Shelf.Lists.Items(
        id = id,
        title = title,
        list = items.mapNotNull { it.releases?.items?.firstOrNull()?.toAlbum(cropCovers) },
        more = if (items.size > 3) more else null
    )
}

fun Releases.toShelf(id: String, title: String, more: Feed<Shelf>, cropCovers: Boolean): Shelf? {
    if (items.isNullOrEmpty()) return null
    return Shelf.Lists.Items(
        id = id,
        title = title,
        list = items.mapNotNull { it.toAlbum(cropCovers) },
        more = if (items.size > 3) more else null
    )
}

private fun ItemsV2.toShelf(
    id: String,
    title: String,
    more: Feed<Shelf>,
    cropCovers: Boolean,
): Shelf? {
    if (items.isNullOrEmpty()) return null
    return Shelf.Lists.Items(
        id = id,
        title = title,
        list = items.mapNotNull { it.toMediaItem(cropCovers) },
        more = if (items.size > 3) more else null
    )
}

fun TracksV2.toTrackShelf(id: String, title: String, cropCovers: Boolean): Shelf? {
    if (items.isNullOrEmpty()) return null
    return Shelf.Lists.Tracks(
        id = id,
        title = title,
        list = items.mapNotNull { it.track?.toTrack(cropCovers) }
    )
}

fun pagedItemsV2(
    cropCovers: Boolean, block: suspend (offset: Int) -> ItemsV2,
): PagedData<Shelf> {
    var count = 0L
    return paged { offset ->
        val res = block(offset)
        val items = res.items?.mapNotNull { it.toMediaItem(cropCovers)?.toShelf() } ?: emptyList()
        val total = res.totalCount ?: 0
        count += items.size
        items to if (count < total) count else null
    }
}

fun pagedArtists(
    cropCovers: Boolean, block: suspend (offset: Int) -> Artists,
): PagedData<Shelf> {
    var count = 0L
    return paged { offset ->
        val res = block(offset)
        val items =
            res.items?.mapNotNull { it.toArtist(null, cropCovers)?.toShelf() } ?: emptyList()
        val total = res.totalCount ?: 0
        count += items.size
        items to if (count < total) count else null
    }
}

fun pagedAlbums(
    cropCovers: Boolean, block: suspend (offset: Int) -> Albums,
): PagedData<Shelf> {
    var count = 0L
    return paged { offset ->
        val res = block(offset)
        val items = res.items?.map {
            it.releases?.items?.firstOrNull()?.toAlbum(cropCovers)!!
        } ?: emptyList()
        val total = res.totalCount ?: 0
        count += items.size
        items.map { it.toShelf() } to if (count < total) count else null
    }
}

fun IArtist.toShelves(queries: Queries, cropCovers: Boolean): List<Shelf> {
    val uri = uri ?: return emptyList()
    return listOfNotNull(
        discography?.topTracks?.toTrackShelf("${uri}_top_tracks", "Top Tracks", cropCovers),
        discography?.latest?.toAlbum(cropCovers)?.toShelf(),
        discography?.popularReleasesAlbums?.toShelf(
            "${uri}_popular",
            "Popular Albums",
            pagedAlbums(cropCovers) {
                queries.queryArtistDiscographyAll(uri, it)
                    .json.data.artistUnion.discography?.all!!
            }.toFeed(),
            cropCovers
        ),
        relatedContent?.featuringV2?.toShelf(
            "${uri}_featuring",
            "Featuring ${profile?.name}",
            pagedItemsV2(cropCovers) {
                queries.queryArtistFeaturing(uri, it)
                    .json.data.artistUnion.relatedContent?.featuringV2!!
            }.toFeed(),
            cropCovers
        ),
        discography?.albums?.toShelf(
            "${uri}_albums",
            "Albums",
            pagedAlbums(cropCovers) {
                queries.queryArtistDiscographyAlbums(uri, it)
                    .json.data.artistUnion.discography?.albums!!
            }.toFeed(),
            cropCovers
        ),
        discography?.singles?.toShelf(
            "${uri}_singles",
            "Singles",
            pagedAlbums(cropCovers) {
                queries.queryArtistDiscographySingles(uri, it)
                    .json.data.artistUnion.discography?.singles!!
            }.toFeed(),
            cropCovers
        ),
        discography?.compilations?.toShelf(
            "${uri}_compilations",
            "Compilations",
            pagedAlbums(cropCovers) {
                queries.queryArtistDiscographyCompilations(uri, it)
                    .json.data.artistUnion.discography?.compilations!!
            }.toFeed(),
            cropCovers
        ),
        profile?.playlistsV2?.toShelf(
            "${uri}_playlists",
            "Playlists",
            pagedItemsV2(cropCovers) {
                queries.queryArtistPlaylists(uri, it)
                    .json.data.artistUnion.profile?.playlistsV2!!
            }.toFeed(),
            cropCovers
        ),
        relatedContent?.appearsOn?.toShelf(
            "${uri}_appears_on",
            "Appears On",
            pagedAlbums(cropCovers) {
                queries.queryArtistAppearsOn(uri, it)
                    .json.data.artistUnion.relatedContent?.appearsOn!!
            }.toFeed(),
            cropCovers
        ),
        relatedContent?.discoveredOnV2?.toShelf(
            "${uri}_discovered_on",
            "Discovered On",
            pagedItemsV2(cropCovers) {
                queries.queryArtistDiscoveredOn(uri, it)
                    .json.data.artistUnion.relatedContent?.discoveredOnV2!!
            }.toFeed(),
            cropCovers
        ),
        relatedContent?.relatedArtists?.toShelf(
            "${uri}_related_artists",
            "Artist",
            "Artist",
            pagedArtists(cropCovers) {
                queries.queryArtistRelated(uri, it)
                    .json.data.artistUnion.relatedContent?.relatedArtists!!
            }.toFeed(),
            cropCovers
        )
    )
}

val likedPlaylist = Playlist(
    "spotify:collection:tracks",
    "Liked Songs",
    true,
    cover = "https://misc.scdn.co/liked-songs/liked-songs-300.png".toImageHolder()
)

fun Wrapper.toMediaItem(cropCovers: Boolean) = when (uri) {
    "spotify:user:@:collection" -> likedPlaylist
    else -> data?.toMediaItem(cropCovers)
}

fun Item.toMediaItem(cropCovers: Boolean): EchoMediaItem? {
    return when (this) {
        is Item.Album -> toAlbum(cropCovers)

        is Item.PreRelease -> Album(
            id = preReleaseContent?.uri ?: return null,
            title = preReleaseContent.name ?: return null,
            type = Album.Type.PreRelease,
            artists = preReleaseContent.artists.toArtists(null, cropCovers),
            cover = preReleaseContent.coverArt?.toImageHolder(cropCovers)
        )

        is Item.PseudoPlaylist -> toPlaylist(cropCovers)

        is Item.Playlist -> toPlaylist(cropCovers)

        is Item.Artist -> toArtist("Artist", cropCovers)

        is Item.Track -> toTrack(cropCovers)

        is Item.Episode -> Track(
            id = uri ?: return null,
            title = name ?: return null,
            type = Track.Type.Podcast,
            cover = coverArt?.toImageHolder(cropCovers),
            description = description?.removeHtml(),
            album = podcastV2?.data?.toAlbum(cropCovers),
            isExplicit = contentRating?.label == Label.EXPLICIT,
            duration = duration?.totalMilliseconds,
            releaseDate = releaseDate?.toDate(),
        )

        is Item.Podcast -> toAlbum(cropCovers)

        is Item.Chapter -> Track(
            id = uri ?: return null,
            title = name ?: return null,
            cover = coverArt?.toImageHolder(cropCovers),
            description = description?.removeHtml(),
            album = audiobookV2?.data?.toAlbum(cropCovers),
            isExplicit = contentRating?.label == Label.EXPLICIT,
            duration = duration?.totalMilliseconds,
        )

        is Item.Audiobook -> toAlbum(cropCovers)

        is Item.User -> Artist(
            id = uri ?: return null,
            name = displayName ?: name ?: username ?: return null,
            subtitle = "User",
            cover = avatar?.toImageHolder(cropCovers)
        )

        is Item.BrowseClientFeature -> null
        is Item.BrowseSectionContainer -> null
        is Item.Genre -> null
        is Item.Folder -> null
        is Item.Merch -> null
        is Item.NotFound -> null
        is Item.RestrictedContent -> null
        is Item.BrowseSpacesHub -> null
        is Item.GenericError -> null
        is Item.DiscoveryFeed -> null
    }
}

val htmlRegex = Regex("<[^>]*>")
private fun String?.removeHtml(): String? {
    return this?.replace(htmlRegex, "")
}

private fun Artwork.toImageHolder(cropCovers: Boolean): ImageHolder? {
    return this.sources.sortedBy { it.height }.middleOrNull()?.url?.toImageHolder(crop = cropCovers)
}

private fun <T> List<T>.middleOrNull() = getOrNull(size.ceilDiv(2)) ?: lastOrNull()
private fun Int.ceilDiv(other: Int) = ceil(this.toDouble() / other).toInt()

fun <T : Any> paged(
    load: suspend (offset: Int) -> Pair<List<T>, Long?>,
) = PagedData.Continuous { cont ->
    val offset = cont?.toInt() ?: 0
    val (data, next) = load(offset)
    Page(data, next?.toString())
}

fun ItemsItem.toBrowseCategory(queries: Queries, cropCovers: Boolean): Shelf.Category? {
    val uri = uri
    val item = content.data
    if (item !is Item.BrowseSectionContainer) return null
    return Shelf.Category(
        id = uri,
        title = item.data?.cardRepresentation?.title?.transformedLabel!!,
//        image = item.data.cardRepresentation.artwork?.toImageHolder(),
        backgroundColor = item.data.cardRepresentation.backgroundColor?.hex,
        feed = paged {
            val sections = queries.browsePage(uri, it).json.data.browse.sections
            val next = sections.pagingInfo?.nextOffset
            sections.toShelves(queries, cropCovers) to next
        }.toFeed()
    )
}

fun ProfileAttributes.toUser() = User(
    id = data.me.profile.uri!!,
    name = data.me.profile.name!!,
    cover = data.me.profile.avatar?.sources?.lastOrNull()?.url?.toImageHolder()
)

private val filteredCategory = listOf("PODCASTS", "AUDIOBOOKS")
fun SearchDesktop.SearchV2.toShelvesAndTabs(
    query: String,
    queries: Queries,
    cropCovers: Boolean,
): Pair<PagedData<Shelf>, List<Tab>> {
    val tabs = listOf(Tab("ALL", "All")) + chipOrder?.items?.map { chip ->
        Tab(chip.typeName!!, chip.typeName.lowercase().replaceFirstChar { it.uppercaseChar() })
    }!!

    val shelves = listOfNotNull(
        topResultsV2?.itemsV2?.firstOrNull()?.item?.toMediaItem(cropCovers)?.toShelf(),
        tracksV2?.items?.toTrackShelf("${query}_songs", "Songs", cropCovers),
        topResultsV2?.featured?.toMediaShelf("${query}_featured", "Featured", cropCovers),
        artists?.toMediaShelf("${query}_artists", "Artists", cropCovers),
        albumsV2?.toMediaShelf("${query}_albums", "Albums", cropCovers),
        playlists?.toMediaShelf("${query}_playlists", "Playlists", cropCovers),
        podcasts?.toMediaShelf("${query}_podcasts", "Podcasts", cropCovers),
        episodes?.toMediaShelf("${query}_episodes", "Episodes", cropCovers),
        users?.toMediaShelf("${query}_users", "Users", cropCovers),
        genres?.toCategoryShelf("${query}_genres", "Genres", queries, cropCovers)
    )
    return PagedData.Single { shelves } to tabs.filter { it.id !in filteredCategory }
}

private fun SearchDesktop.SearchItems.toCategoryShelf(
    id: String, title: String, queries: Queries, cropCovers: Boolean,
): Shelf? {
    if (items.isNullOrEmpty()) return null
    val items = items.mapNotNull { it.toGenreCategory(queries, cropCovers) }
    return Shelf.Lists.Categories(
        id = id,
        title = title,
        list = items,
        type = Shelf.Lists.Type.Grid
    )
}

fun SearchDesktop.SearchItems?.toItemShelves(cropCovers: Boolean): Pair<List<Shelf>, Long?> {
    if (this == null || items == null) return emptyList<Shelf>() to null
    val items = items
    val next = pagingInfo?.nextOffset
    return items.mapNotNull { item ->
        item.data?.toMediaItem(cropCovers)?.toShelf()
    } to next
}

fun SearchDesktop.TracksV2?.toItemShelves(
    cropCovers: Boolean,
): Pair<List<Shelf>, Long?> {
    if (this == null || items == null) return emptyList<Shelf>() to null
    val items = items
    val next = pagingInfo?.nextOffset
    return items.mapNotNull { item ->
        item.item?.data?.toTrack(cropCovers)?.toShelf()
    } to next
}

fun SearchDesktop.SearchItems?.toCategoryShelves(
    queries: Queries,
    cropCovers: Boolean,
): Pair<List<Shelf>, Long?> {
    if (this == null || items == null) return emptyList<Shelf>() to null
    val items = items
    val next = pagingInfo?.nextOffset
    return items.mapNotNull { item ->
        item.toGenreCategory(queries, cropCovers)
    } to next
}

private fun Wrapper.toGenreCategory(
    queries: Queries, cropCovers: Boolean,
): Shelf.Category? {
    val item = data
    if (item !is Item.Genre) return null
    val uri = item.uri!!
    return Shelf.Category(
        id = uri,
        title = item.name ?: return null,
        feed = paged {
            val sections = queries.browsePage(uri, it).json.data.browse.sections
            val next = sections.pagingInfo?.nextOffset
            sections.toShelves(queries, cropCovers) to next
        }.toFeed()
    )
}

private fun SearchDesktop.SearchItems.toMediaShelf(id: String, title: String, cropCovers: Boolean) =
    items?.toMediaShelf(id, title, cropCovers)

private fun List<Wrapper>?.toMediaShelf(id: String, title: String, cropCovers: Boolean): Shelf? {
    if (this.isNullOrEmpty()) return null
    return Shelf.Lists.Items(
        id = id,
        title = title,
        list = mapNotNull { it.toMediaItem(cropCovers) }
    )
}

private fun List<SearchDesktop.TrackWrapperWrapper>?.toTrackShelf(
    id: String,
    title: String,
    cropCovers: Boolean,
): Shelf? {
    if (this.isNullOrEmpty()) return null
    return Shelf.Lists.Tracks(
        id = id,
        title = title,
        list = mapNotNull { it.item?.data?.toTrack(cropCovers) }
    )
}

fun Canvas.toStreamable(): Streamable? {
    val canvas = data?.trackUnion?.canvas ?: return null
    if (!canvas.type.orEmpty().startsWith("VIDEO")) return null
    val url = canvas.url ?: return null
    return Streamable.background(
        id = url,
        title = canvas.type ?: return null,
        quality = 0
    )
}

private fun Metadata4Track.Date.toReleaseDate() =
    if (year != null) Date(year, month, day) else null

fun Metadata4Track.Format.show(
    hasPremium: Boolean, supportsPlayPlay: Boolean, showWidevineStreams: Boolean,
) = when (this) {
    Metadata4Track.Format.OGG_VORBIS_320 -> hasPremium && supportsPlayPlay
    Metadata4Track.Format.OGG_VORBIS_160 -> supportsPlayPlay
    Metadata4Track.Format.OGG_VORBIS_96 -> supportsPlayPlay
    Metadata4Track.Format.MP4_256_DUAL -> false
    Metadata4Track.Format.MP4_128_DUAL -> false
    Metadata4Track.Format.MP4_256 -> hasPremium && showWidevineStreams
    Metadata4Track.Format.MP4_128 -> showWidevineStreams
    Metadata4Track.Format.AAC_24 -> false
    Metadata4Track.Format.MP3_96 -> false
}

fun Metadata4Track.toTrack(
    hasPremium: Boolean,
    supportsPlayPlay: Boolean,
    showWidevineStreams: Boolean,
    canvas: Streamable?,
): Track {
    val id = "spotify:track:${Base62.encode(gid!!)}"
    val title = name!!
    val streamables = (file ?: alternative?.firstOrNull()?.file).orEmpty().mapNotNull {
        val fileId = it.fileId ?: return@mapNotNull null
        val format = it.format ?: return@mapNotNull null

        if (!format.show(hasPremium, supportsPlayPlay, showWidevineStreams)) return@mapNotNull null
        Streamable.server(
            id = fileId,
            quality = format.quality,
            title = format.name.replace('_', ' '),
            extras = mapOf(
                "format" to it.format.name,
                "gid" to gid
            )
        )
    }
    val alb = album?.let { album ->
        val gid = album.gid ?: return@let null
        val albumId = Base62.encode(gid)
        Album(
            id = "spotify:album:$albumId",
            title = album.name ?: return@let null,
            cover = album.coverGroup?.image?.lastOrNull()?.fileId?.let {
                "https://i.scdn.co/image/$it".toImageHolder()
            },
            artists = album.artist?.mapNotNull {
                val artistGid = it.gid ?: return@mapNotNull null
                val artistId = Base62.encode(artistGid)
                Artist("spotify:artist:$artistId", it.name ?: return@mapNotNull null)
            }.orEmpty(),
            label = album.label,
            releaseDate = album.date?.toReleaseDate(),
        )
    }
    return Track(
        id = id,
        title = title,
        cover = alb?.cover,
        streamables = if (canvas != null) streamables + canvas else streamables,
        duration = duration,
        artists = artistWithRole?.mapNotNull {
            val gid = it.artistGid ?: return@mapNotNull null
            val artistId = Base62.encode(gid)
            val name = it.artistName ?: return@mapNotNull null
            val subtitle = it.role?.split('_')?.joinToString(" ") { s ->
                s.lowercase().replaceFirstChar { char -> char.uppercaseChar() }
            }
            Artist("spotify:artist:$artistId", name, subtitle = subtitle)
        } ?: listOf(),
        album = alb,
        albumOrderNumber = number,
        albumDiscNumber = discNumber,
        isrc = externalId?.firstOrNull()?.id,
        releaseDate = alb?.releaseDate,
        description = album?.label,
        isExplicit = explicit == true,
    )
}

fun UserProfileView.toArtist(): Artist? {
    return Artist(
        id = uri ?: return null,
        name = name ?: return null,
        cover = imageUrl?.toImageHolder(),
        bio = "Total Public Playlists Count : $totalPublicPlaylistsCount",
        extras = mapOf(
            "followers" to followersCount?.toString().orEmpty(),
        )
    )
}

fun UserProfileView.toShelf(): Shelf? {
    if (publicPlaylists.isNullOrEmpty()) return null
    val playlists = publicPlaylists.mapNotNull {
        val owner = it.ownerUri?.let { uri ->
            Artist(id = uri, name = it.ownerName!!)
        }
        Playlist(
            id = it.uri ?: return@mapNotNull null,
            title = it.name ?: return@mapNotNull null,
            false,
            cover = it.imageUrl?.toImageHolder(),
            authors = listOfNotNull(owner)
        )
    }
    return Shelf.Lists.Items("${uri}_public_playlists", "Public Playlists", playlists)
}

fun UserFollowers.toShelf(id: String, title: String): Shelf? {
    if (profiles.isNullOrEmpty()) return null
    val users = profiles.mapNotNull {
        Artist(
            id = it.uri ?: return@mapNotNull null,
            name = it.name ?: return@mapNotNull null,
            cover = it.imageUrl?.toImageHolder()
        )
    }
    return Shelf.Lists.Items(id, title, users)
}

fun pagedLibrary(
    queries: Queries, filter: String? = null, folderUri: String? = null, cropCovers: Boolean,
) = paged { offset ->
    val res = queries.libraryV3(offset, filter, folderUri)
    val library = res.json.data?.me?.libraryV3!!
    val shelves = library.items.mapNotNull { it.toShelf(queries, cropCovers) }
    val page = library.pagingInfo!!
    val next = page.offset!! + page.limit!!
    shelves to if (library.totalCount!! > next) next else null
}

fun LibraryV3.Item.toShelf(queries: Queries, cropCovers: Boolean): Shelf? {
    return if (item?.typename == "LibraryFolderResponseWrapper") {
        val folder = item.data as Item.Folder
        val folderUri = folder.uri ?: return null
        Shelf.Category(
            folderUri, folder.name!!,
            pagedLibrary(queries, null, folderUri, cropCovers).toFeed()
        )
    } else item?.data?.toMediaItem(cropCovers)?.toShelf()
}

fun editablePlaylists(
    queries: Queries,
    track: String?,
    folderUri: String? = null,
    cropCovers: Boolean,
): PagedData<Pair<Playlist, Boolean>> =
    paged { offset ->
        val res = queries.editablePlaylists(
            offset, folderUri, track ?: "spotify:track:3z5lNLYtGC6LmvrxSbCQgd"
        ).json.data.me.editablePlaylists!!
        val playlists = res.items.mapNotNull {
            when (val item = it.item.data) {
                is Item.PseudoPlaylist -> {
                    val curated = it.curates ?: return@mapNotNull null
                    val playlist = item.toPlaylist(cropCovers) ?: return@mapNotNull null
                    listOfNotNull(playlist to curated)
                }

                is Item.Playlist -> {
                    val curated = it.curates ?: return@mapNotNull null
                    val playlist = item.toPlaylist(cropCovers) ?: return@mapNotNull null
                    listOfNotNull(playlist to curated)
                }

                is Item.Folder -> {
                    val uri = item.uri ?: return@mapNotNull null
                    editablePlaylists(queries, track, uri, cropCovers).loadAll()
                }

                else -> null
            }
        }.flatten()
        val page = res.pagingInfo!!
        val next = page.offset!! + page.limit!!
        playlists to if (res.totalCount!! > next) next else null
    }