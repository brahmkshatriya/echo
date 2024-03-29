package dev.brahmkshatriya.echo.data

import android.content.Context
import androidx.core.net.toUri
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.pagedFlow
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItemsContainer
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.SettingCategory
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.data.offline.AlbumResolver
import dev.brahmkshatriya.echo.data.offline.ArtistResolver
import dev.brahmkshatriya.echo.data.offline.TrackResolver
import dev.brahmkshatriya.echo.data.offline.URI
import dev.brahmkshatriya.echo.data.offline.sortedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class OfflineExtension(val context: Context) : ExtensionClient(), SearchClient, TrackClient,
    HomeFeedClient, AlbumClient, ArtistClient, RadioClient {

    override val metadata = ExtensionMetadata(
        id = "echo_offline",
        name = "Offline",
        version = "1.0.0",
        description = "Local media library",
        author = "Echo",
        iconUrl = null
    )

    override val settings = listOf(
        SettingCategory(
            title = "General", key = "general", items = listOf(
                SettingList(
                    title = "Sorting",
                    key = "sorting",
                    entryTitles = listOf("Date Added", "A to Z", "Z to A", "Year"),
                    entryValues = listOf("date", "a_to_z", "z_to_a", "year"),
                    defaultEntryIndex = 1
                )
            )
        )
    )

    private val sorting get() = preferences.getString("sorting", "a_to_z")!!
    private val artistResolver = ArtistResolver(context)
    private val albumResolver = AlbumResolver(context)
    private val trackResolver = TrackResolver(context)

    override suspend fun quickSearch(query: String): List<QuickSearchItem> = listOf()

    override suspend fun search(query: String): Flow<PagingData<MediaItemsContainer>> = flow {
        val trimmed = query.trim()
        val albums =
            albumResolver.search(trimmed, 0, 50, sorting).map { it.toMediaItem() }.toMutableList()
                .ifEmpty { null }
        val tracks =
            trackResolver.search(trimmed, 0, 50, sorting).map { it.toMediaItem() }.toMutableList()
                .ifEmpty { null }
        val artists =
            artistResolver.search(trimmed, 0, 50, sorting).map { it.toMediaItem() }.toMutableList()
                .ifEmpty { null }

        val exactMatch = artists?.firstOrNull { it.artist.name.equals(trimmed, true) }.also {
            artists?.remove(it)
        } ?: albums?.firstOrNull { it.album.title.equals(trimmed, true) }.also {
            albums?.remove(it)
        } ?: tracks?.firstOrNull { it.track.title.equals(trimmed, true) }.also {
            tracks?.remove(it)
        }
        val result: MutableList<MediaItemsContainer> = listOfNotNull(tracks?.let {
            val track = it.firstOrNull()?.track ?: return@let null
            track.title to it.toMediaItemsContainer("Tracks")
        }, albums?.let {
            val album = it.firstOrNull()?.album ?: return@let null
            album.title to it.toMediaItemsContainer("Albums")
        }, artists?.let {
            val artist = it.firstOrNull()?.artist ?: return@let null
            artist.name to it.toMediaItemsContainer("Artists")
        }).sortedBy(query) { it.first }.map { it.second }.toMutableList()
        exactMatch?.toMediaItemsContainer()?.let { result.add(0, it) }
        emit(PagingData.from(result))
    }

    override suspend fun getTrack(id: String): Track? {
        return trackResolver.get(id.toUri())
    }

    override suspend fun getStreamableAudio(streamable: Streamable): StreamableAudio {
        return trackResolver.getStreamable(streamable)
    }

    override suspend fun getHomeGenres(): List<Genre> {
        return listOf("All", "Tracks", "Albums", "Artists").map { Genre(it, it) }
    }

    override suspend fun getHomeFeed(genre: StateFlow<Genre?>) =
        pagedFlow { page: Int, pageSize: Int ->
            when (genre.value?.id) {
                "Tracks" -> trackResolver.getAll(page, pageSize, sorting)
                    .map { MediaItemsContainer.TrackItem(it) }

                "Albums" -> albumResolver.getAll(page, pageSize, sorting)
                    .map { MediaItemsContainer.AlbumItem(it) }

                "Artists" -> artistResolver.getAll(page, pageSize, sorting)
                    .map { MediaItemsContainer.ArtistItem(it) }

                else -> if (page == 0) {
                    val albums = albumResolver.getShuffled(pageSize).map { it.toMediaItem() }
                        .ifEmpty { null }

                    val albumsFlow = pagedFlow<EchoMediaItem> { inPage: Int, inPageSize: Int ->
                        albumResolver.getAll(inPage, inPageSize, sorting).map { it.toMediaItem() }
                    }

                    val tracks = trackResolver.getShuffled(pageSize).map { it.toMediaItem() }
                        .ifEmpty { null }

                    val tracksFlow = pagedFlow<EchoMediaItem> { inPage: Int, inPageSize: Int ->
                        trackResolver.getAll(inPage, inPageSize, sorting).map { it.toMediaItem() }
                    }

                    val artists = artistResolver.getShuffled(pageSize).map { it.toMediaItem() }
                        .ifEmpty { null }

                    val artistsFlow = pagedFlow<EchoMediaItem> { inPage: Int, inPageSize: Int ->
                        artistResolver.getAll(inPage, inPageSize, sorting).map { it.toMediaItem() }
                    }

                    val result = listOfNotNull(
                        tracks?.toMediaItemsContainer("Tracks", flow = tracksFlow),
                        albums?.toMediaItemsContainer("Albums", flow = albumsFlow),
                        artists?.toMediaItemsContainer("Artists", flow = artistsFlow)
                    )
                    result
                } else {
                    trackResolver.getAll(page - 1, pageSize, sorting)
                        .map { MediaItemsContainer.TrackItem(it) }
                }
            }
        }

    override suspend fun loadAlbum(small: Album): Album {
        return albumResolver.get(small.id.toUri(), trackResolver)
    }

    override suspend fun getMediaItems(album: Album): Flow<PagingData<MediaItemsContainer>> =
        flow {
            val result = album.artists.map { small ->
                val artist = small.name
                val tracks =
                    trackResolver.search(artist, 1, 50, sorting).filter { it.album?.id != album.id }
                        .map { it.toMediaItem() }.ifEmpty { null }
                val albums =
                    albumResolver.search(artist, 1, 50, sorting).filter { it.id != album.id }
                        .map { it.toMediaItem() }.ifEmpty { null }
                listOfNotNull(
                    tracks?.toMediaItemsContainer("More from $artist"),
                    albums?.toMediaItemsContainer("Albums")
                )
            }.flatten()
            emit(PagingData.from(result))
        }

    override suspend fun loadArtist(small: Artist): Artist {
        return artistResolver.get(small.id.toUri())
    }

    override suspend fun getMediaItems(artist: Artist): Flow<PagingData<MediaItemsContainer>> =
        flow {

            val albums = albumResolver.getByArtist(artist, 0, 10, sorting).map { it.toMediaItem() }
                .ifEmpty { null }
            val albumFlow = pagedFlow<EchoMediaItem> { page: Int, pageSize: Int ->
                albumResolver.getByArtist(artist, page, pageSize, sorting).map { it.toMediaItem() }
            }

            val tracks = trackResolver.getByArtist(artist, 0, 10, sorting).map { it.toMediaItem() }
                .ifEmpty { null }
            val trackFlow = pagedFlow<EchoMediaItem> { page: Int, pageSize: Int ->
                trackResolver.getByArtist(artist, page, pageSize, sorting).map { it.toMediaItem() }
            }

            val result = listOfNotNull(
                tracks?.toMediaItemsContainer("Tracks", flow = trackFlow),
                albums?.toMediaItemsContainer("Albums", flow = albumFlow)
            )
            emit(PagingData.from(result))
        }

    override suspend fun radio(track: Track): Playlist {
        val albumTracks = track.album?.let { trackResolver.getByAlbum(it, 0, 25, sorting) }
        val trackArtist = track.artists.firstOrNull()
        val artistTracks = trackArtist?.let { trackResolver.getByArtist(it, 0, 25, sorting) }
        val randomTracks = trackResolver.getShuffled(25)

        val tracks =
            listOfNotNull(albumTracks, artistTracks, randomTracks).flatten().distinctBy { it.id }
                .toMutableList()

        tracks.removeIf { it.id == track.id }
        tracks.shuffle()

        return Playlist(
            id = "$URI${track.id}",
            title = "${track.title} Radio",
            cover = null,
            authors = listOf(),
            tracks = tracks,
            creationDate = null,
            duration = tracks.sumOf { it.duration ?: 0 },
            subtitle = "Radio based on ${track.title} by ${track.artists.firstOrNull()?.name}"
        )
    }

    override suspend fun radio(album: Album): Playlist {
        val albumTracks = trackResolver.getByAlbum(album, 0, 25, sorting)
        val randomTracks = trackResolver.getShuffled(25)
        val tracks =
            listOfNotNull(albumTracks, randomTracks).flatten().shuffled().distinctBy { it.id }
        return Playlist(
            id = "$URI${album.id}",
            title = "${album.title} Radio",
            cover = null,
            authors = listOf(),
            tracks = tracks,
            creationDate = null,
            duration = tracks.sumOf { it.duration ?: 0 },
            subtitle = "Radio based on ${album.title}"
        )
    }

    override suspend fun radio(artist: Artist): Playlist {
        val artistTracks = trackResolver.getByArtist(artist, 0, 25, sorting)
        val randomTracks = trackResolver.getShuffled(25)
        val tracks =
            listOfNotNull(artistTracks, randomTracks).flatten().shuffled().distinctBy { it.id }

        return Playlist(
            id = "$URI${artist.id}",
            title = "${artist.name} Radio",
            cover = null,
            authors = listOf(),
            tracks = tracks,
            creationDate = null,
            duration = tracks.sumOf { it.duration ?: 0 },
            subtitle = "Radio based on ${artist.name}"
        )
    }

    override suspend fun radio(playlist: Playlist): Playlist {
        TODO("Not yet implemented")
    }
}