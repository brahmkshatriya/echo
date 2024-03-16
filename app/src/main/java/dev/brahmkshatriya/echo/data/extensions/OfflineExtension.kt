package dev.brahmkshatriya.echo.data.extensions

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItemsContainer
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
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
        id="echo_offline",
        name = "Offline",
        version = "1.0.0",
        description = "Local media library",
        author = "Echo",
        iconUrl = null
    )

    override fun setupPreferenceSettings(preferenceScreen: PreferenceScreen) {
        PreferenceCategory(context).apply {
            title = "General"
            key = "general"
            isIconSpaceReserved = false
            preferenceScreen.addPreference(this)

            ListPreference(context).apply {
                key = "sorting"
                title = "Sorting"
                entries = arrayOf("Date Added", "A to Z", "Z to A", "Year")
                entryValues = arrayOf("date", "a_to_z", "z_to_a", "year")

                addPreference(this)
            }
        }
    }

    private val sorting get() = preferences.getString("sorting", "a_to_z")!!
    val artistResolver = ArtistResolver(context)
    val albumResolver = AlbumResolver(context)
    val trackResolver = TrackResolver(context)

    override suspend fun quickSearch(query: String): List<QuickSearchItem> = listOf()

    override suspend fun search(query: String): Flow<PagingData<MediaItemsContainer>> = flow {
        val trimmed = query.trim()
        val albums = albumResolver.search(trimmed, 0, 50, sorting).map { it.toMediaItem() }.toMutableList()
            .ifEmpty { null }
        val tracks = trackResolver.search(trimmed, 0, 50, sorting).map { it.toMediaItem() }.toMutableList()
            .ifEmpty { null }
        val artists = artistResolver.search(trimmed, 0, 50, sorting).map { it.toMediaItem() }.toMutableList()
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

    override suspend fun getTrack(uri: Uri): Track? {
        return trackResolver.get(uri)
    }

    override suspend fun getStreamable(track: Track): StreamableAudio {
        return trackResolver.getStream(track).toAudio()
    }

    override suspend fun getHomeGenres(): List<String> {
        return listOf(
            "All", "Tracks", "Albums", "Artists"
        )
    }

    override suspend fun getHomeFeed(genre: StateFlow<String?>) =
        object : PagedFlow<MediaItemsContainer>() {
            override fun loadItems(page: Int, pageSize: Int): List<MediaItemsContainer> =
                when (genre.value) {
                    "Tracks" -> trackResolver.getAll(page, pageSize, sorting)
                        .map { MediaItemsContainer.TrackItem(it) }

                    "Albums" -> albumResolver.getAll(page, pageSize, sorting)
                        .map { MediaItemsContainer.AlbumItem(it) }

                    "Artists" -> artistResolver.getAll(page, pageSize, sorting)
                        .map { MediaItemsContainer.ArtistItem(it) }

                    else -> if (page == 0) {
                        val albums =
                            albumResolver.getShuffled(pageSize).map { it.toMediaItem() }
                                .ifEmpty { null }

                        val albumsFlow = object : PagedFlow<EchoMediaItem>() {
                            override fun loadItems(page: Int, pageSize: Int): List<EchoMediaItem> =
                                albumResolver.getAll(page, pageSize, sorting).map { it.toMediaItem() }
                        }.getFlow()

                        val tracks =
                            trackResolver.getShuffled(pageSize).map { it.toMediaItem() }
                                .ifEmpty { null }

                        val tracksFlow = object : PagedFlow<EchoMediaItem>() {
                            override fun loadItems(page: Int, pageSize: Int): List<EchoMediaItem> =
                                trackResolver.getAll(page, pageSize, sorting).map { it.toMediaItem() }
                        }.getFlow()

                        val artists =
                            artistResolver.getShuffled(pageSize).map { it.toMediaItem() }
                                .ifEmpty { null }

                        val artistsFlow = object : PagedFlow<EchoMediaItem>() {
                            override fun loadItems(page: Int, pageSize: Int): List<EchoMediaItem> =
                                artistResolver.getAll(page, pageSize, sorting).map { it.toMediaItem() }
                        }.getFlow()

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
        }.getFlow()

    override suspend fun loadAlbum(small: Album.Small): Album.Full {
        return albumResolver.get(small.uri, trackResolver)
    }

    override suspend fun getMediaItems(album: Album.Full): Flow<PagingData<MediaItemsContainer>> =
        flow {
            val artist = album.artist.name
            val tracks = trackResolver.search(artist, 1, 50, sorting).filter { it.album?.uri != album.uri }
                .map { it.toMediaItem() }.ifEmpty { null }
            val albums = albumResolver.search(artist, 1, 50, sorting).filter { it.uri != album.uri }
                .map { it.toMediaItem() }.ifEmpty { null }
            val result = listOfNotNull(
                tracks?.toMediaItemsContainer("More from $artist"),
                albums?.toMediaItemsContainer("Albums")
            )
            emit(PagingData.from(result))
        }

    override suspend fun loadArtist(small: Artist.Small): Artist.Full {
        return artistResolver.get(small.uri)
    }

    override suspend fun getMediaItems(artist: Artist.Full): Flow<PagingData<MediaItemsContainer>> =
        flow {

            val albums =
                albumResolver.getByArtist(artist, 0, 10, sorting).map { it.toMediaItem() }.ifEmpty { null }
            val albumFlow = object : PagedFlow<EchoMediaItem>() {
                override fun loadItems(page: Int, pageSize: Int): List<EchoMediaItem> =
                    albumResolver.getByArtist(artist, page, pageSize, sorting).map { it.toMediaItem() }
            }.getFlow()

            val tracks =
                trackResolver.getByArtist(artist, 0, 10, sorting).map { it.toMediaItem() }.ifEmpty { null }
            val trackFlow = object : PagedFlow<EchoMediaItem>() {
                override fun loadItems(page: Int, pageSize: Int): List<EchoMediaItem> =
                    trackResolver.getByArtist(artist, page, pageSize, sorting).map { it.toMediaItem() }
            }.getFlow()

            val result = listOfNotNull(
                tracks?.toMediaItemsContainer("Tracks", flow = trackFlow),
                albums?.toMediaItemsContainer("Albums", flow = albumFlow)
            )
            emit(PagingData.from(result))
        }

    override suspend fun radio(track: Track): Playlist.Full {
        val albumTracks = track.album?.let { trackResolver.getByAlbum(it, 0, 25, sorting) }
        val trackArtist = track.artists.firstOrNull()
        val artistTracks = trackArtist?.let { trackResolver.getByArtist(it, 0, 25, sorting) }
        val randomTracks = trackResolver.getShuffled(25)

        val tracks = listOfNotNull(albumTracks, artistTracks, randomTracks)
            .flatten()
            .distinctBy { it.uri }
            .toMutableList()

        tracks.removeIf { it.uri == track.uri }
        tracks.shuffle()

        return Playlist.Full(
            uri = Uri.parse("$URI${track.uri}"),
            title = "${track.title} Radio",
            cover = null,
            author = null,
            tracks = tracks,
            creationDate = null,
            duration = tracks.sumOf { it.duration ?: 0 },
            description = "Radio based on ${track.title} by ${track.artists.firstOrNull()?.name}"
        )
    }

    override suspend fun radio(album: Album.Small): Playlist.Full {
        val albumTracks = trackResolver.getByAlbum(album, 0, 25, sorting)
        val randomTracks = trackResolver.getShuffled(25)
        val tracks = listOfNotNull(albumTracks,  randomTracks)
            .flatten()
            .shuffled()
            .distinctBy { it.uri }
        return Playlist.Full(
            uri = Uri.parse("$URI${album.uri}"),
            title = "${album.title} Radio",
            cover = null,
            author = null,
            tracks = tracks,
            creationDate = null,
            duration = tracks.sumOf { it.duration ?: 0 },
            description = "Radio based on ${album.title}"
        )
    }

    override suspend fun radio(artist: Artist.Small): Playlist.Full {
        val artistTracks = trackResolver.getByArtist(artist, 0, 25, sorting)
        val randomTracks = trackResolver.getShuffled(25)
        val tracks = listOfNotNull(artistTracks, randomTracks)
            .flatten()
            .shuffled()
            .distinctBy { it.uri }

        return Playlist.Full(
            uri = Uri.parse("$URI${artist.uri}"),
            title = "${artist.name} Radio",
            cover = null,
            author = null,
            tracks = tracks,
            creationDate = null,
            duration = tracks.sumOf { it.duration ?: 0 },
            description = "Radio based on ${artist.name}"
        )
    }

    override suspend fun radio(playlist: Playlist.Small): Playlist.Full {
        TODO("Not yet implemented")
    }
}