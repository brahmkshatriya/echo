package dev.brahmkshatriya.echo.offline

import android.content.Context
import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.EditPlayerListenerClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.addSongToPlaylist
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.createPlaylist
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.deletePlaylist
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.editPlaylist
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.moveSongInPlaylist
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.removeSongFromPlaylist
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache

class OfflineExtension(val context: Context) : ExtensionClient(), HomeFeedClient, TrackClient,
    AlbumClient, ArtistClient, PlaylistClient, RadioClient, SearchClient, LibraryClient,
    EditPlayerListenerClient {
    override val metadata = ExtensionMetadata(
        id = "echo_offline",
        name = "Offline",
        description = "Offline extension",
        version = "1.0.0",
        author = "Echo",
        iconUrl = null
    )
    override val settings: List<Setting> = listOf()
    lateinit var library: MediaStoreUtils.LibraryStoreClass
    private fun find(artist: Artist) =
        library.artistMap[artist.id.toLongOrNull()]

    private fun find(album: Album) =
        library.albumList.find { it.id == album.id.toLong() }

    override suspend fun onExtensionSelected() {
        library = MediaStoreUtils.getAllSongs(context)
    }

    override suspend fun getHomeGenres() = listOf(
        "All", "Songs", "Albums", "Artists", "Genres"
    ).map { Genre(it, it) }

    private fun List<MediaItemsContainer>.toPaged() = PagedData.Single { this }

    override fun getHomeFeed(genre: Genre?): PagedData<MediaItemsContainer> {
        fun List<EchoMediaItem>.sorted() = sortedBy { it.title.lowercase() }
            .map { it.toMediaItemsContainer() }.toPaged()
        return when (genre?.id) {
            "Songs" -> library.songList.map { it.toTrack().toMediaItem() }.sorted()
            "Albums" -> library.albumList.map { it.toAlbum().toMediaItem() }.sorted()
            "Artists" -> library.artistMap.values.map { it.toArtist().toMediaItem() }.sorted()
            "Genres" -> library.genreList.map { it.toContainer() }.toPaged()
            else -> run {
                val recentlyAdded = library.songList.sortedByDescending {
                    it.mediaMetadata.extras!!.getLong("ModifiedDate")
                }.map { it.toTrack().toMediaItem() }
                val albums = library.albumList.map {
                    it.toAlbum().toMediaItem()
                }.shuffled()
                val artists = library.artistMap.values.map {
                    it.toArtist().toMediaItem()
                }.shuffled()
                listOf(
                    MediaItemsContainer.Category("Recently Added",
                        recentlyAdded.take(10),
                        null,
                        PagedData.Single { recentlyAdded }),
                    MediaItemsContainer.Category("Albums",
                        albums.take(10),
                        null,
                        PagedData.Single { albums }),
                    MediaItemsContainer.Category("Artists",
                        artists.take(10),
                        null,
                        PagedData.Single { artists })
                ) + library.songList.map {
                    it.toTrack().toMediaItem().toMediaItemsContainer()
                }
            }.toPaged()
        }

    }

    override suspend fun loadTrack(track: Track) = track
    override suspend fun getStreamableAudio(streamable: Streamable) = streamable.id.toAudio()
    override suspend fun getStreamableVideo(streamable: Streamable) = throw IllegalAccessException()

    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> = PagedData.Single {
        listOfNotNull(
            track.album?.let { find(it)?.toAlbum()?.toMediaItem()?.toMediaItemsContainer() }
        ) + getArtistsWithCategories(track.artists) { it.mediaId != track.id }
    }

    override suspend fun loadAlbum(small: Album) =
        find(small)!!.toAlbum()

    private fun getArtistsWithCategories(
        artists: List<Artist>, filter: (MediaItem) -> Boolean
    ) = artists.map { small ->
        val artist = find(small)
        val category = artist?.songList?.filter {
                filter(it)
            }?.map { it.toTrack().toMediaItem() }?.ifEmpty { null }?.let { tracks ->
                val items = tracks as List<EchoMediaItem>
                MediaItemsContainer.Category(
                    context.getString(R.string.more_by_artist, small.name), items,
                    null, PagedData.Single { items }
                )
            }
        listOfNotNull(artist.toArtist().toMediaItem().toMediaItemsContainer(), category)
    }.flatten()

    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> = PagedData.Single {
        getArtistsWithCategories(album.artists) {
            it.mediaMetadata.extras?.getLong("AlbumId") != album.id.toLong()
        }
    }

    override suspend fun loadArtist(small: Artist) =
        find(small)!!.toArtist()

    override fun getMediaItems(artist: Artist) = PagedData.Single<MediaItemsContainer> {
        find(artist)?.run {
            val tracks = songList.map { it.toTrack().toMediaItem() }.ifEmpty { null }
            val albums = albumList.map { it.toAlbum().toMediaItem() }.ifEmpty { null }
            listOfNotNull(
                tracks?.let {
                    MediaItemsContainer.Category(
                        "Songs", it, null, PagedData.Single { tracks }
                    )
                },
                albums?.let {
                    MediaItemsContainer.Category(
                        "Albums", it, null, PagedData.Single { albums }
                    )
                }
            )
        } ?: listOf()
    }

    override suspend fun loadPlaylist(playlist: Playlist) =
        library.playlistList.find { it.id == playlist.id.toLong() }!!.toPlaylist()

    override fun getMediaItems(playlist: Playlist) = PagedData.Single<MediaItemsContainer> {
        emptyList()
    }

    override suspend fun radio(track: Track): Playlist {
        val albumTracks = track.album?.let { loadAlbum(it) }?.tracks
        val artistTracks = track.artists.map { artist ->
            find(artist)?.songList ?: emptyList()
        }.flatten().map { it.toTrack() }
        val randomTracks = library.songList.shuffled().take(25).map { it.toTrack() }
        val allTracks =
            listOfNotNull(albumTracks, artistTracks, randomTracks).flatten().distinctBy { it.id }
                .toMutableList()
        allTracks.removeIf { it.id == track.id }
        allTracks.shuffle()

        return Playlist(
            id = "",
            title = "${track.title} Radio",
            cover = null,
            isEditable = false,
            authors = listOf(),
            tracks = allTracks,
            creationDate = null,
            subtitle = "Radio based on ${track.title} by ${track.artists.firstOrNull()?.name}"
        )
    }

    override suspend fun radio(album: Album): Playlist {
        val tracks = album.tracks
        val randomTracks = library.songList.shuffled().take(25).map { it.toTrack() }
        val allTracks = (tracks + randomTracks).distinctBy { it.id }.toMutableList()
        allTracks.shuffle()

        return Playlist(
            id = "",
            title = "${album.title} Radio",
            cover = null,
            isEditable = false,
            authors = listOf(),
            tracks = allTracks,
            creationDate = null,
            subtitle = "Radio based on ${album.title}"
        )
    }

    override suspend fun radio(artist: Artist): Playlist {
        val tracks = find(artist)?.songList?.map { it.toTrack() } ?: emptyList()
        val randomTracks = library.songList.shuffled().take(25).map { it.toTrack() }
        val allTracks = (tracks + randomTracks).distinctBy { it.id }.toMutableList()
        allTracks.shuffle()

        return Playlist(
            id = "",
            title = "${artist.name} Radio",
            cover = null,
            isEditable = false,
            authors = listOf(),
            tracks = allTracks,
            creationDate = null,
            subtitle = "Radio based on ${artist.name}"
        )
    }

    override suspend fun radio(playlist: Playlist): Playlist {
        val tracks = playlist.tracks
        val randomTracks = library.songList.shuffled().take(25).map { it.toTrack() }
        val allTracks = (tracks + randomTracks).distinctBy { it.id }.toMutableList()
        allTracks.shuffle()

        return Playlist(
            id = "",
            title = "${playlist.title} Radio",
            cover = null,
            isEditable = false,
            authors = listOf(),
            tracks = allTracks,
            creationDate = null,
            subtitle = "Radio based on ${playlist.title}"
        )
    }

    override suspend fun quickSearch(query: String?): List<QuickSearchItem> {
        return if (query.isNullOrBlank()) {
            getHistory().map { QuickSearchItem.SearchQueryItem(it, true) }
        } else listOf()
    }

    override suspend fun searchGenres(query: String?) =
        listOf("All", "Tracks", "Albums", "Artists").map { Genre(it, it) }

    private fun getHistory() = context.getFromCache("search_history", "offline") {
        it.createStringArrayList()
    } ?: emptyList()

    private fun saveInHistory(query: String) {
        val history = getHistory().toMutableList()
        history.add(0, query)
        context.saveToCache("search_history", "offline") { parcel ->
            parcel.writeStringList(history)
        }
    }

    override fun search(query: String?, genre: Genre?) = run {
        query ?: return@run emptyList()
        saveInHistory(query)
        val tracks = library.songList.map { it.toTrack() }.searchBy(query) {
            listOf(it.title, it.album?.title) + it.artists.map { artist -> artist.name }
        }.map { it.first to it.second.toMediaItem() }
        val albums = library.albumList.map { it.toAlbum() }.searchBy(query) {
            listOf(it.title) + it.artists.map { artist -> artist.name }
        }.map { it.first to it.second.toMediaItem() }
        val artists = library.artistMap.values.map { it.toArtist() }.searchBy(query) {
            listOf(it.name)
        }.map { it.first to it.second.toMediaItem() }

        when (genre?.id) {
            "Tracks" -> tracks.map { it.second.toMediaItemsContainer() }
            "Albums" -> albums.map { it.second.toMediaItemsContainer() }
            "Artists" -> artists.map { it.second.toMediaItemsContainer() }
            else -> {
                val items = listOf(
                    "Tracks" to tracks, "Albums" to albums, "Artist" to artists
                ).sortedBy { it.second.firstOrNull()?.first ?: 20 }
                    .map { it.first to it.second.map { pair -> pair.second } }
                    .filter { it.second.isNotEmpty() }

                val exactMatch = items.firstNotNullOfOrNull {
                    it.second.find { item -> item.title.contains(query, true) }
                }?.toMediaItemsContainer()

                val containers = items.map { (title, items) ->
                    MediaItemsContainer.Category(title, items, null, PagedData.Single { items })
                }

                listOf(listOfNotNull(exactMatch), containers).flatten()
            }
        }
    }.toPaged()

    override suspend fun getLibraryGenres(): List<Genre> = listOf(
        "Playlists", "Folders"
    ).map { Genre(it, it) }

    override fun getLibraryFeed(genre: Genre?): PagedData<MediaItemsContainer> {
        return when (genre?.id) {
            "Folders" -> library.folderStructure.folderList.entries.first().value.toContainer(null).more!!
            else -> {
                library.playlistList.map { it.toPlaylist().toMediaItem().toMediaItemsContainer() }
                    .toPaged()
            }
        }
    }

    override suspend fun listEditablePlaylists() = library.playlistList.map { it.toPlaylist() }

    override suspend fun likeTrack(track: Track, liked: Boolean): Boolean {
        val playlist = library.likedPlaylist.toPlaylist()
        if (liked) context.addSongToPlaylist(playlist.id.toLong(), track.id.toLong(), 0)
        else context.removeSongFromPlaylist(playlist.id.toLong(), track.id.toLong())
        library = MediaStoreUtils.getAllSongs(context)
        return liked
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val id = context.createPlaylist(title)
        library = MediaStoreUtils.getAllSongs(context)
        return library.playlistList.find { it.id == id }!!.toPlaylist()
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        context.deletePlaylist(playlist.id.toLong())
        library = MediaStoreUtils.getAllSongs(context)
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?
    ) {
        context.editPlaylist(playlist.id.toLong(), title)
    }

    override suspend fun addTracksToPlaylist(playlist: Playlist, index: Int?, tracks: List<Track>) {
        tracks.forEach {
            context.addSongToPlaylist(
                playlist.id.toLong(),
                it.id.toLong(),
                index ?: playlist.tracks.size
            )
        }
    }

    override suspend fun removeTracksFromPlaylist(playlist: Playlist, tracks: List<Track>) {
        tracks.forEach {
            context.removeSongFromPlaylist(playlist.id.toLong(), it.id.toLong())
        }
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist, fromIndex: Int, toIndex: Int
    ) {
        context.moveSongInPlaylist(playlist.id.toLong(), fromIndex, toIndex)
    }

    override suspend fun onEnterPlaylistEditor(playlist: Playlist) {}
    override suspend fun onExitPlaylistEditor(playlist: Playlist) {
        library = MediaStoreUtils.getAllSongs(context)
    }
}