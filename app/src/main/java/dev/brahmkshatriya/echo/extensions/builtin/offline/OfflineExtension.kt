package dev.brahmkshatriya.echo.extensions.builtin.offline

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingMultipleChoice
import dev.brahmkshatriya.echo.common.settings.SettingSlider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getSettings
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.addSongToPlaylist
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.createPlaylist
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.deletePlaylist
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.editPlaylist
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.moveSongInPlaylist
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.removeSongFromPlaylist
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import java.io.File

@OptIn(UnstableApi::class)
class OfflineExtension(
    private val context: Context,
) : ExtensionClient, HomeFeedClient, TrackClient, AlbumClient, ArtistClient, PlaylistClient,
    RadioClient, SearchFeedClient, LibraryFeedClient, TrackLikeClient, PlaylistEditorListenerClient,
    SettingsChangeListenerClient {

    companion object {
        val metadata = Metadata(
            className = "OfflineExtension",
            path = "",
            importType = ImportType.BuiltIn,
            type = ExtensionType.MUSIC,
            id = "echo-offline",
            name = "Offline",
            description = "An extension for all your downloaded files.",
            version = "v${BuildConfig.VERSION_CODE}",
            author = "Echo",
            icon = R.drawable.ic_offline.toResourceImageHolder(),
        )
    }

    override suspend fun onSettingsChanged(settings: Settings, key: String?) {
        refreshLibrary()
    }

    override val settingItems: List<Setting>
        get() = listOf(
            SettingSwitch(
                context.getString(R.string.refresh_library_on_reload),
                "refresh_library",
                context.getString(R.string.refresh_library_on_reload_summary),
                false
            ),
            SettingSlider(
                context.getString(R.string.duration_filter),
                "limit_value",
                context.getString(R.string.duration_filter_summary),
                10,
                0,
                120,
                10
            ),
            SettingMultipleChoice(
                context.getString(R.string.blacklist_folders),
                "blacklist_folders",
                context.getString(R.string.blacklist_folders_summary),
                library.folders.toList(),
                library.folders.toList()
            ),
            SettingTextInput(
                context.getString(R.string.blacklist_folder_keywords),
                "blacklist_keywords",
                context.getString(R.string.blacklist_folder_keywords_summary)
            )
        )

    private val settings = getSettings(context, metadata)
    override fun setSettings(settings: Settings) {}
    private val refreshLibrary
        get() = settings.getBoolean("refresh_library") ?: true

    private var library = MediaStoreUtils.getAllSongs(context, settings)
    private fun refreshLibrary() {
        library = MediaStoreUtils.getAllSongs(context, settings)
    }


    private fun find(artist: Artist) =
        library.artistMap[artist.id.toLongOrNull()]

    private fun find(album: Album) =
        library.albumList.find { it.id == album.id.toLong() }

    private fun find(playlist: Playlist) =
        library.playlistList.find { it.id == playlist.id.toLong() }


    override suspend fun onExtensionSelected() {}

    override suspend fun getHomeTabs() = listOf<Tab>()
    private fun List<Shelf>.toFeed(
        playShuffle: Boolean = false,
        search: Boolean = false,
        filter: Boolean = false
    ) = PagedData.Single { this }.toFeed(
        showPlayButton = playShuffle,
        showShuffleButton = playShuffle,
        showSearchButton = search,
        showFilterButton = filter
    )


    override fun getHomeFeed(tab: Tab?): Feed {
        if (refreshLibrary) refreshLibrary()

        val recentlyAdded = library.songList.sortedByDescending {
            it.extras["addDate"]?.toLongOrNull()
        }.map { it }
        val albums = library.albumList.map {
            it.toAlbum().toMediaItem()
        }.shuffled()
        val artists = library.artistMap.values.map {
            it.toArtist().toMediaItem()
        }.shuffled()

        val recent = if (recentlyAdded.isNotEmpty()) Shelf.Lists.Tracks(
            context.getString(R.string.recently_added),
            recentlyAdded.take(6),
            more = PagedData.Single { recentlyAdded }.takeIf { albums.size > 6 }
        ) else null

        val albumShelf = if (albums.isNotEmpty()) Shelf.Lists.Items(
            context.getString(R.string.albums),
            albums.take(10),
            more =
                PagedData.Single<EchoMediaItem> { albums }.takeIf { albums.size > 10 }
        ) else null

        val artistsShelf = if (artists.isNotEmpty()) Shelf.Lists.Items(
            context.getString(R.string.artists),
            artists.take(10),
            more =
                PagedData.Single<EchoMediaItem> { artists }.takeIf { albums.size > 10 }
        ) else null
        val data = listOfNotNull(recent, albumShelf, artistsShelf) +
                library.songList.map { it.toMediaItem().toShelf() }
        return data.toFeed()
    }

    override suspend fun loadTrack(track: Track) = track.copy(
        isLiked = library.likedPlaylist?.songList.orEmpty().any { it.id == track.id }
    )

    override suspend fun loadStreamableMedia(streamable: Streamable, isDownload: Boolean) =
        Uri.fromFile(File(streamable.id)).toString().toSource().toMedia()

    override fun getShelves(track: Track): PagedData<Shelf> =
        PagedData.Single { listOf() }

    override suspend fun loadAlbum(album: Album) =
        find(album)!!.toAlbum()

    override fun loadTracks(album: Album): PagedData<Track> = PagedData.Single {
        find(album)!!.songList.sortedBy { it.extras["trackNumber"]?.toLongOrNull() }.map { it }
    }

    private fun getArtistsWithCategories(
        artists: List<Artist>, filter: (Track) -> Boolean
    ) = artists.map { small ->
        val artist = find(small)
        val category = artist?.songList?.filter {
            filter(it)
        }?.map { it.toMediaItem() }?.ifEmpty { null }?.let { tracks ->
            val items = tracks as List<EchoMediaItem>
            Shelf.Lists.Items(
                context.getString(R.string.more_by_x, small.name), items,
                more = PagedData.Single { items }
            )
        }
        listOfNotNull(artist.toArtist().toMediaItem().toShelf(), category)
    }.flatten()

    override fun getShelves(album: Album): PagedData<Shelf> = PagedData.Single {
        getArtistsWithCategories(album.artists) { it.album?.id != album.id }
    }

    override suspend fun loadArtist(artist: Artist) =
        find(artist)!!.toArtist()

    override fun getShelves(artist: Artist) = PagedData.Single<Shelf> {
        find(artist)?.run {
            val tracks = songList.map { it.toMediaItem() }.ifEmpty { null }
            val albums = albumList.map { it.toAlbum().toMediaItem() }.ifEmpty { null }
            listOfNotNull(
                tracks?.let {
                    Shelf.Lists.Items(
                        context.getString(R.string.songs), it, more = PagedData.Single { tracks }
                    )
                },
                albums?.let {
                    Shelf.Lists.Items(
                        context.getString(R.string.albums), it, more = PagedData.Single { albums }
                    )
                }
            )
        } ?: listOf()
    }

    override suspend fun loadPlaylist(playlist: Playlist) =
        if (playlist.id == "cached") playlist else find(playlist)!!.toPlaylist()

    override fun loadTracks(playlist: Playlist): PagedData<Track> = PagedData.Single {
        find(playlist)!!.songList.map { it }
    }

    override fun getShelves(playlist: Playlist) = PagedData.Single<Shelf> {
        emptyList()
    }

    private fun createRadioPlaylist(mediaItem: EchoMediaItem): Radio {
        val id = "radio_${mediaItem.hashCode()}"
        val title = mediaItem.title
        return Radio(
            id = id,
            title = context.getString(R.string.x_radio, title),
            extras = mapOf("mediaItem" to mediaItem.toJson())
        )
    }

    override fun loadTracks(radio: Radio): PagedData<Track> = PagedData.Single {
        val mediaItem = radio.extras["mediaItem"]!!.toData<EchoMediaItem>()
        when (mediaItem) {
            is EchoMediaItem.Lists.AlbumItem -> {
                val album = mediaItem.album
                val tracks = loadTracks(album).loadAll().asSequence()
                    .map { it.artists }.flatten()
                    .map { artist -> find(artist)?.songList?.map { it }!! }.flatten()
                    .filter { it.album?.id != album.id }.take(25)

                val randomTracks = library.songList.shuffled().take(25).map { it }
                (tracks + randomTracks).distinctBy { it.id }.toMutableList()
            }

            is EchoMediaItem.Lists.PlaylistItem -> {
                val playlist = mediaItem.playlist
                val tracks = loadTracks(playlist).loadAll()
                val randomTracks = library.songList.shuffled().take(25).map { it }
                (tracks + randomTracks).distinctBy { it.id }.toMutableList()
            }

            is EchoMediaItem.Profile.ArtistItem -> {
                val artist = mediaItem.artist
                val tracks = find(artist)?.songList?.map { it } ?: emptyList()
                val randomTracks = library.songList.shuffled().take(25).map { it }
                (tracks + randomTracks).distinctBy { it.id }.toMutableList()
            }

            is EchoMediaItem.TrackItem -> {
                val track = mediaItem.track
                val albumTracks = track.album?.let { loadTracks(loadAlbum(it)).loadAll() }
                val artistTracks = track.artists.map { artist ->
                    find(artist)?.songList ?: emptyList()
                }.flatten().map { it }
                val randomTracks = library.songList.shuffled().take(25).map { it }
                val allTracks =
                    listOfNotNull(albumTracks, artistTracks, randomTracks).flatten()
                        .distinctBy { it.id }
                        .toMutableList()
                allTracks.removeIf { it.id == track.id }
                allTracks
            }

            else -> throw IllegalAccessException()
        }.shuffled()
    }

    override suspend fun radio(track: Track, context: EchoMediaItem?) =
        createRadioPlaylist(track.toMediaItem())

    override suspend fun radio(album: Album) = createRadioPlaylist(album.toMediaItem())
    override suspend fun radio(artist: Artist) = createRadioPlaylist(artist.toMediaItem())
    override suspend fun radio(playlist: Playlist) = createRadioPlaylist(playlist.toMediaItem())
    override suspend fun radio(user: User): Radio = throw IllegalAccessException()

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        return if (query.isBlank()) {
            getHistory().map { QuickSearchItem.Query(it, true) }
        } else listOf()
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        val history = getHistory().toMutableList()
        history.remove(item.title)
        context.saveToCache("search_history", history, "offline")
    }

    private fun getHistory() = context.getFromCache<List<String>>("search_history", "offline")
        ?.distinct()?.take(5)
        ?: emptyList()

    private fun saveInHistory(query: String) {
        val history = getHistory().toMutableList()
        history.add(0, query)
        context.saveToCache("search_history", history, "offline")
    }

    override suspend fun searchTabs(query: String) =
        (if (query.isBlank()) listOf("Songs", "Albums", "Artists", "Genres")
        else listOf("All", "Tracks", "Albums", "Artists")).map { Tab(it, it) }


    private fun List<EchoMediaItem>.sorted() = sortedBy { it.title.lowercase() }
        .map { it.toShelf(false) }.toFeed(filter = true)

    override fun searchFeed(query: String, tab: Tab?): Feed = if (query.isBlank()) {
        if (refreshLibrary) refreshLibrary()
        when (tab?.id) {
            "Albums" -> library.albumList.map { it.toAlbum().toMediaItem() }.sorted()
            "Artists" -> library.artistMap.values.map { it.toArtist().toMediaItem() }.sorted()
            "Genres" -> library.genreList.map { it.toShelf() }.toFeed()
            else -> library.songList.sortedByDescending {
                it.extras["addDate"]?.toLongOrNull()
            }.map { it.toMediaItem().toShelf() }.toFeed(playShuffle = true, filter = true)
        }
    } else {
        saveInHistory(query)
        val tracks = library.songList.map { it }.searchBy(query) {
            listOf(it.title, it.album?.title) + it.artists.map { artist -> artist.name }
        }.map { it.first to it.second.toMediaItem() }
        val albums = library.albumList.map { it.toAlbum() }.searchBy(query) {
            listOf(it.title) + it.artists.map { artist -> artist.name }
        }.map { it.first to it.second.toMediaItem() }
        val artists = library.artistMap.values.map { it.toArtist() }.searchBy(query) {
            listOf(it.name)
        }.map { it.first to it.second.toMediaItem() }

        when (tab?.id) {
            "Tracks" -> tracks.map { it.second.toShelf() }
            "Albums" -> albums.map { it.second.toShelf() }
            "Artists" -> artists.map { it.second.toShelf() }
            else -> {
                val items = listOf(
                    "Tracks" to tracks, "Albums" to albums, "Artist" to artists
                ).sortedBy { it.second.firstOrNull()?.first ?: 20 }
                    .map { it.first to it.second.map { pair -> pair.second } }
                    .filter { it.second.isNotEmpty() }

                val exactMatch = items.firstNotNullOfOrNull {
                    it.second.find { item -> item.title.contains(query, true) }
                }?.toShelf()

                val containers = items.map { (title, items) ->
                    Shelf.Lists.Items(title, items, more = PagedData.Single { items })
                }

                listOf(listOfNotNull(exactMatch), containers).flatten()
            }
        }.toFeed()
    }

    override suspend fun getLibraryTabs(): List<Tab> = listOf(
        "Playlists", "Folders"
    ).map { Tab(it, it) }

    override fun getLibraryFeed(tab: Tab?): Feed {
        if (refreshLibrary) refreshLibrary()
        val bruh: PagedData<Shelf> = when (tab?.id) {
            "Folders" -> library.folderStructure.folderList.entries.firstOrNull()?.value
                ?.toShelf(context, null)?.items ?: PagedData.Single { listOf() }

            else -> PagedData.Single {
                library.playlistList.map { it.toPlaylist().toMediaItem().toShelf() }
            }
        }
        return bruh.toFeed(showFilterButton = true, showSearchButton = true)
    }

    override suspend fun listEditablePlaylists(track: Track?) = library.playlistList.map {
        val has = it.songList.any { song -> song.id == track?.id }
        it.toPlaylist() to has
    }

    override suspend fun likeTrack(track: Track, isLiked: Boolean) {
        val library = library
        val playlist = library.likedPlaylist?.id
            ?: throw ClientException.NotSupported("Couldn't create Liked Playlist")
        if (isLiked) context.addSongToPlaylist(playlist, track.id.toLong(), 0)
        else {
            val index = library.likedPlaylist.songList.indexOfFirst { it.id == track.id }
            context.removeSongFromPlaylist(playlist, index)
        }
        refreshLibrary()
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val id = context.createPlaylist(title)
        refreshLibrary()
        return library.playlistList.find { it.id == id }!!.toPlaylist()
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        context.deletePlaylist(playlist.id.toLong())
        refreshLibrary()
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?
    ) {
        context.editPlaylist(playlist.id.toLong(), title)
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>
    ) {
        new.forEach {
            context.addSongToPlaylist(playlist.id.toLong(), it.id.toLong(), index)
        }
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist, tracks: List<Track>, indexes: List<Int>
    ) {
        indexes.forEach { index ->
            context.removeSongFromPlaylist(playlist.id.toLong(), index)
        }
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int
    ) {
        val song = tracks[fromIndex].id.toLong()
        context.moveSongInPlaylist(playlist.id.toLong(), song, fromIndex, toIndex)
    }

    override suspend fun onEnterPlaylistEditor(playlist: Playlist, tracks: List<Track>) {}
    override suspend fun onExitPlaylistEditor(playlist: Playlist, tracks: List<Track>) {
        refreshLibrary()
    }
}