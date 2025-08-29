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
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
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
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import java.io.File

@OptIn(UnstableApi::class)
class OfflineExtension(
    private val context: Context,
) : ExtensionClient, HomeFeedClient, TrackClient, AlbumClient, ArtistClient, PlaylistClient,
    RadioClient, LibraryFeedClient, LikeClient, PlaylistEditorListenerClient,
    SearchFeedClient, SettingsChangeListenerClient {

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

    override suspend fun getSettingItems(): List<Setting> {
        val folders = getLibrary().folders
        return listOf(
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
                folders.toList(),
                folders.toList()
            ),
            SettingTextInput(
                context.getString(R.string.blacklist_folder_keywords),
                "blacklist_keywords",
                context.getString(R.string.blacklist_folder_keywords_summary)
            )
        )
    }

    private val settings = getSettings(context, metadata)
    override fun setSettings(settings: Settings) {}
    private val refreshLibrary
        get() = settings.getBoolean("refresh_library") ?: true

    private var _library: MediaStoreUtils.LibraryStoreClass? = null
    private suspend fun getLibrary(): MediaStoreUtils.LibraryStoreClass {
        if (_library == null) _library = MediaStoreUtils.getAllSongs(context, settings)
        return _library!!
    }

    private suspend fun refreshLibrary() {
        _library = MediaStoreUtils.getAllSongs(context, settings)
    }

    private suspend fun find(artist: Artist) =
        getLibrary().artistMap[artist.id.toLongOrNull()]

    private suspend fun find(album: Album) =
        getLibrary().albumList.find { it.id == album.id.toLong() }

    private suspend fun find(playlist: Playlist) =
        getLibrary().playlistList.find { it.id == playlist.id.toLong() }

    private fun List<EchoMediaItem>.toShelves(buttons: Feed.Buttons? = null): Feed<Shelf> =
        map { it.toShelf() }.toFeed(buttons)

    override suspend fun loadHomeFeed() = Feed(listOf()) {
        if (refreshLibrary) refreshLibrary()
        val library = getLibrary()
        val recentlyAdded = library.songList.sortedByDescending {
            it.extras["addDate"]?.toLongOrNull()
        }.map { it }
        val albums = library.albumList.map {
            it.toAlbum()
        }.shuffled()
        val artists = library.artistMap.values.map {
            it.toArtist()
        }.shuffled()

        val recent = if (recentlyAdded.isNotEmpty()) Shelf.Lists.Tracks(
            "recents",
            context.getString(R.string.recently_added),
            recentlyAdded.take(9),
            more = recentlyAdded.toShelves().takeIf { recentlyAdded.size > 9 }
        ) else null

        val albumShelf = if (albums.isNotEmpty()) Shelf.Lists.Items(
            "albums",
            context.getString(R.string.albums),
            albums.take(10),
            more = albums.toShelves().takeIf { albums.size > 10 }
        ) else null

        val artistsShelf = if (artists.isNotEmpty()) Shelf.Lists.Items(
            "artists",
            context.getString(R.string.artists),
            artists.take(10),
            more = artists.toShelves().takeIf { artists.size > 10 }
        ) else null

        val data = PagedData.Single {
            listOfNotNull(recent, albumShelf, artistsShelf) + library.songList.map { it.toShelf() }
        }
        data.toFeedData(
            Feed.Buttons(
                showSearch = false, showSort = true, showPlayAndShuffle = true
            )
        )
    }

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track = track

    override suspend fun loadStreamableMedia(streamable: Streamable, isDownload: Boolean) =
        Uri.fromFile(File(streamable.id)).toString().toSource(isLive = false).toMedia()

    override suspend fun loadFeed(track: Track): Feed<Shelf>? = null

    override suspend fun loadAlbum(album: Album) =
        find(album)!!.toAlbum()

    override suspend fun loadTracks(album: Album): Feed<Track> = PagedData.Single {
        find(album)!!.songList.sortedBy { it.extras["trackNumber"]?.toLongOrNull() }.map { it }
    }.toFeed()

    override suspend fun loadFeed(album: Album) =
        getArtistsWithCategories(album.artists) { it.album?.id != album.id }

    private suspend fun getArtistsWithCategories(
        artists: List<Artist>, filter: (Track) -> Boolean,
    ) = artists.map { small ->
        val artist = find(small)
        val category = artist?.songList?.filter {
            filter(it)
        }?.map { it }?.ifEmpty { null }?.let { tracks ->
            Shelf.Lists.Items(
                small.id,
                context.getString(R.string.more_by_x, small.name),
                tracks,
                more = tracks.toShelves()
            )
        }
        listOfNotNull(artist.toArtist().toShelf(), category)
    }.flatten().toFeed()

    override suspend fun loadArtist(artist: Artist) =
        find(artist)!!.toArtist()

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return find(artist)!!.run {
            val tracks = songList.ifEmpty { null }?.toList()
            val albums = albumList.map { it.toAlbum() }.ifEmpty { null }
            listOfNotNull(
                tracks?.let {
                    val id = "${artist.id}_tracks"
                    listOf(
                        Shelf.Lists.Items(
                            id,
                            context.getString(R.string.songs) + " (${it.size})",
                            listOf(),
                            more = tracks.toShelves().takeIf { tracks.size >= 10 }
                        )
                    )
                },
                tracks?.take(10)?.map { it.toShelf() },
                albums?.let {
                    val id = "${artist.id}_albums"
                    listOf(
                        Shelf.Lists.Items(
                            id,
                            context.getString(R.string.albums) + " (${it.size})",
                            it,
                            more = albums.toShelves().takeIf { albums.size >= 4 }
                        )
                    )
                }
            ).flatten().toFeed(Feed.Buttons(showPlayAndShuffle = true, customTrackList = tracks))
        }
    }

    override suspend fun loadPlaylist(playlist: Playlist) =
        if (playlist.id == "cached") playlist else find(playlist)!!.toPlaylist()

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> = PagedData.Single {
        find(playlist)!!.songList.map { it }
    }.toFeed()

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? = null

    override suspend fun loadRadio(radio: Radio) = radio

    override suspend fun loadTracks(radio: Radio): Feed<Track> = PagedData.Single {
        val mediaItem = radio.extras["mediaItem"]!!.toData<EchoMediaItem>()
        val library = getLibrary()
        when (mediaItem) {
            is Album -> {
                val tracks = loadTracks(mediaItem).loadAll().asSequence()
                    .map { it.artists }.flatten().map { artist ->
                        library.artistMap[artist.id.toLongOrNull()]?.songList?.map { it }!!
                    }.flatten().filter { it.album?.id != mediaItem.id }.take(25)

                val randomTracks = library.songList.shuffled().take(25).map { it }
                (tracks + randomTracks).distinctBy { it.id }.toMutableList()
            }

            is Playlist -> {
                val tracks = loadTracks(mediaItem).loadAll()
                val randomTracks = getLibrary().songList.shuffled().take(25).map { it }
                (tracks + randomTracks).distinctBy { it.id }.toMutableList()
            }

            is Artist -> {
                val tracks = find(mediaItem)?.songList?.map { it } ?: emptyList()
                val randomTracks = getLibrary().songList.shuffled().take(25).map { it }
                (tracks + randomTracks).distinctBy { it.id }.toMutableList()
            }

            is Track -> {
                val albumTracks = mediaItem.album?.let { loadTracks(loadAlbum(it)).loadAll() }
                val artistTracks = mediaItem.artists.map { artist ->
                    find(artist)?.songList ?: emptyList()
                }.flatten().map { it }
                val randomTracks = getLibrary().songList.shuffled().take(25).map { it }
                val allTracks =
                    listOfNotNull(albumTracks, artistTracks, randomTracks).flatten()
                        .distinctBy { it.id }
                        .toMutableList()
                allTracks.removeIf { it.id == mediaItem.id }
                allTracks
            }

            else -> throw IllegalAccessException()
        }.shuffled()
    }.toFeed()

    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        val id = "radio_${item.hashCode()}"
        val title = item.title
        return Radio(
            id = id,
            title = this.context.getString(R.string.x_radio, title),
            extras = mapOf("mediaItem" to item.toJson())
        )
    }

    private fun List<EchoMediaItem>.sorted() =
        sortedBy { it.title.lowercase() }.map { it.toShelf() }

    private fun List<Shelf>.toPair(buttons: Feed.Buttons? = null) =
        PagedData.Single { this }.toFeedData(buttons)

    private fun Pair<List<Shelf>, Boolean>.toFeed() =
        first.toPair(if (second) Feed.Buttons(false, showSort = true) else null)

    override suspend fun loadSearchFeed(query: String) = Feed(
        (if (query.isBlank()) listOf("Songs", "Albums", "Artists", "Genres")
        else listOf("All", "Songs", "Albums", "Artists")).map { Tab(it, it) }
    ) { tab ->
        if (query.isBlank()) {
            if (refreshLibrary) refreshLibrary()
            when (tab?.id) {
                "Albums" -> getLibrary().albumList.map { it.toAlbum() }.sorted().toPair(
                    Feed.Buttons(showSearch = false, showSort = true)
                )

                "Artists" -> getLibrary().artistMap.values.map { it.toArtist() }.sorted().toPair(
                    Feed.Buttons(showSearch = false, showSort = true)
                )

                "Genres" -> getLibrary().genreList.map { it.toShelf() }.toPair()
                else -> getLibrary().songList.sortedByDescending {
                    it.extras["addDate"]?.toLongOrNull()
                }.map { it.toShelf() }.toPair(
                    Feed.Buttons(
                        showSearch = false, showSort = true, showPlayAndShuffle = true
                    )
                )
            }
        } else {
            val tracks = getLibrary().songList.map { it }.searchBy(query) {
                listOf(it.title, it.album?.title) + it.artists.map { artist -> artist.name }
            }
            val albums = getLibrary().albumList.map { it.toAlbum() }.searchBy(query) {
                listOf(it.title) + it.artists.map { artist -> artist.name }
            }
            val artists = getLibrary().artistMap.values.map { it.toArtist() }.searchBy(query) {
                listOf(it.name)
            }

            when (tab?.id) {
                "Songs" -> tracks.map { it.second.toShelf() } to true
                "Albums" -> albums.map { it.second.toShelf() } to true
                "Artists" -> artists.map { it.second.toShelf() } to true
                else -> {
                    val items = listOf(
                        "Songs" to tracks, "Albums" to albums, "Artist" to artists
                    ).sortedBy { it.second.firstOrNull()?.first ?: 20 }
                        .map { it.first to it.second.map { pair -> pair.second } }
                        .filter { it.second.isNotEmpty() }

                    val exactMatch = items.firstNotNullOfOrNull {
                        it.second.find { item -> item.title.contains(query, true) }
                    }?.toShelf()

                    val containers = items.map { (title, items) ->
                        val id = "${query}_$title"
                        Shelf.Lists.Items(id, title, items, more = items.toShelves())
                    }

                    listOf(listOfNotNull(exactMatch), containers).flatten() to false
                }
            }.toFeed()
        }
    }

    override suspend fun loadLibraryFeed() = Feed(
        listOf("Playlists", "Folders").map { Tab(it, it) }
    ) { tab ->
        if (refreshLibrary) refreshLibrary()
        val pagedData: PagedData<Shelf> = when (tab?.id) {
            "Folders" -> getLibrary().folderStructure.folderList.entries.firstOrNull()?.value
                ?.toShelf(context, null)?.feed?.getPagedData?.invoke(null)?.pagedData
                ?: PagedData.Single { listOf() }

            else -> PagedData.Single {
                getLibrary().playlistList.map { it.toPlaylist().toShelf() }
            }
        }
        pagedData.toFeedData(Feed.Buttons())
    }

    override suspend fun listEditablePlaylists(track: Track?) = getLibrary().playlistList.map {
        val has = it.songList.any { song -> song.id == track?.id }
        it.toPlaylist() to has
    }

    override suspend fun likeItem(item: EchoMediaItem, shouldLike: Boolean) {
        val library = getLibrary()
        val playlist = library.likedPlaylist?.id
            ?: throw ClientException.NotSupported("Couldn't create Liked Playlist")
        if (shouldLike) context.addSongToPlaylist(playlist, item.id.toLong(), 0)
        else {
            val index = library.likedPlaylist.songList.indexOfFirst { it.id == item.id }
            context.removeSongFromPlaylist(playlist, index)
        }
        refreshLibrary()
    }

    override suspend fun isItemLiked(item: EchoMediaItem) =
        getLibrary().likedPlaylist?.songList?.find { it.id == item.id } != null

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val id = context.createPlaylist(title)
        refreshLibrary()
        return getLibrary().playlistList.find { it.id == id }!!.toPlaylist()
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        context.deletePlaylist(playlist.id.toLong())
        refreshLibrary()
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?,
    ) {
        context.editPlaylist(playlist.id.toLong(), title)
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>,
    ) {
        new.forEach {
            context.addSongToPlaylist(playlist.id.toLong(), it.id.toLong(), index)
        }
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist, tracks: List<Track>, indexes: List<Int>,
    ) {
        indexes.forEach { index ->
            context.removeSongFromPlaylist(playlist.id.toLong(), index)
        }
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int,
    ) {
        val song = tracks[fromIndex].id.toLong()
        context.moveSongInPlaylist(playlist.id.toLong(), song, fromIndex, toIndex)
    }

    override suspend fun onEnterPlaylistEditor(playlist: Playlist, tracks: List<Track>) {}
    override suspend fun onExitPlaylistEditor(playlist: Playlist, tracks: List<Track>) {
        refreshLibrary()
    }
}