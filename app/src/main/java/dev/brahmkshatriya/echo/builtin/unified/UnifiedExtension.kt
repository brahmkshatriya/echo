package dev.brahmkshatriya.echo.builtin.unified

import android.content.Context
import androidx.room.Room
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.getSettings
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class UnifiedExtension(
    private val context: Context
) : ExtensionClient, MusicExtensionsProvider, HomeFeedClient, SearchFeedClient, LibraryFeedClient,
    PlaylistClient, AlbumClient, UserClient, ArtistClient, RadioClient, LyricsClient,
    TrackClient, TrackLikeClient, SaveToLibraryClient, PlaylistEditClient {

    companion object {
        const val UNIFIED_ID = "unified"
        const val EXTENSION_ID = "extension_id"
        val metadata = Metadata(
            "UnifiedExtension",
            "",
            ImportType.BuiltIn,
            UNIFIED_ID,
            "Unified Extension",
            "1.0.0",
            "Beta extension to test unified home feed",
            "Echo",
            enabled = false
        )

        suspend inline fun <reified C, T> Extension<*>.client(block: C.() -> T): T = runCatching {
            val client = instance.value().getOrThrow() as? C
                ?: throw ClientException.NotSupported(C::class.java.name)
            client.block()
        }.getOrElse { throw it.toAppException(this) }

        private fun List<Extension<*>>.get(id: String?) =
            find { it.id == id } ?: throw Exception("Extension $id not found")

        val Map<String, String>.extensionId
            get() = this[EXTENSION_ID] ?: throw Exception("Extension id not found")

        private fun Track.withExtensionId(id: String) = copy(
            extras = extras + mapOf(EXTENSION_ID to id),
            album = album?.withExtensionId(id),
            artists = artists.map { it.withExtensionId(id) },
            streamables = streamables.map { it.copy(extras = it.extras + mapOf(EXTENSION_ID to id)) }
        )

        private fun Album.withExtensionId(id: String) = copy(
            artists = artists.map { it.withExtensionId(id) },
            extras = extras + mapOf(EXTENSION_ID to id)
        )

        private fun Artist.withExtensionId(id: String) = copy(
            extras = extras + mapOf(EXTENSION_ID to id)
        )

        private fun Playlist.withExtensionId(id: String) = copy(
            isEditable = false,
            authors = authors.map { it.withExtensionId(id) },
            extras = extras + mapOf(EXTENSION_ID to id)
        )

        private fun User.withExtensionId(id: String) = copy(
            extras = extras + mapOf(EXTENSION_ID to id)
        )

        private fun Radio.withExtensionId(id: String) = copy(
            extras = extras + mapOf(EXTENSION_ID to id)
        )

        private fun EchoMediaItem.withExtensionId(id: String): EchoMediaItem {
            return when (this) {
                is EchoMediaItem.Lists.AlbumItem -> album.withExtensionId(id).toMediaItem()
                is EchoMediaItem.Lists.PlaylistItem -> playlist.withExtensionId(id).toMediaItem()
                is EchoMediaItem.Lists.RadioItem -> radio.withExtensionId(id).toMediaItem()
                is EchoMediaItem.Profile.ArtistItem -> artist.withExtensionId(id).toMediaItem()
                is EchoMediaItem.Profile.UserItem -> user.withExtensionId(id).toMediaItem()
                is EchoMediaItem.TrackItem -> track.withExtensionId(id).toMediaItem()
            }
        }

        private fun Shelf.Item.withExtensionId(id: String) = copy(
            media = media.withExtensionId(id)
        )

        private fun Shelf.Category.withExtensionId(extension: Extension<*>): Shelf.Category =
            copy(items = items?.injectExtensionId(extension))

        fun PagedData<Shelf>.injectExtensionId(extension: Extension<*>): PagedData<Shelf> =
            map { result ->
                val id = extension.id
                val list = result.getOrElse { throw it.toAppException(extension) }
                list.map {
                    when (it) {
                        is Shelf.Category -> it.withExtensionId(extension)
                        is Shelf.Item -> it.withExtensionId(id)
                        is Shelf.Lists.Categories -> it.copy(
                            list = it.list.map { category -> category.withExtensionId(extension) }
                        )

                        is Shelf.Lists.Items -> it.copy(
                            list = it.list.map { item -> item.withExtensionId(id) }
                        )

                        is Shelf.Lists.Tracks -> it.copy(
                            list = it.list.map { track -> track.withExtensionId(id) }
                        )
                    }
                }
            }

        fun PagedData<Track>.injectExtension(extension: Extension<*>): PagedData<Track> =
            map { result ->
                val id = extension.id
                val list = result.getOrElse { throw it.toAppException(extension) }
                list.map { it.withExtensionId(id) }
            }

        private fun Lyrics.withExtensionId(id: String) = copy(
            extras = extras + mapOf(EXTENSION_ID to id)
        )

        fun PagedData<Lyrics>.injectLyricsExtId(extension: Extension<*>) = map { result ->
            val list = result.getOrElse { throw it.toAppException(extension) }
            list.map { it.withExtensionId(extension.id) }
        }
    }

    override val settingItems = listOf(
        SettingSwitch(
            context.getString(R.string.show_tabs),
            "show_tabs",
            context.getString(R.string.show_tab_summary),
            true
        ),
    )

    private val settings = getSettings(context, ExtensionType.MUSIC, metadata)
    override fun setSettings(settings: Settings) {}
    private val showTabs get() = settings.getBoolean("show_tabs") ?: true

    override val requiredMusicExtensions = listOf<String>()
    private var extFlow = MutableStateFlow<List<MusicExtension>?>(null)
    suspend fun extensions() = extFlow.first { it != null }!!
    override fun setMusicExtensions(extensions: List<MusicExtension>) {
        extFlow.value = extensions.filter { it.id != UNIFIED_ID }
    }

    private fun feed(
        extension: Extension<*>,
        loadTabs: suspend () -> List<Tab>,
        getFeed: (Tab?) -> PagedData<Shelf>
    ): PagedData<Shelf> = PagedData.Suspend {
        val tabs = runCatching { loadTabs() }.getOrElse { throw it.toAppException(extension) }
        val loadFeed: (Tab?) -> PagedData<Shelf> =
            { runCatching { getFeed(it) }.getOrElse { throw it.toAppException(extension) } }

        val shelf: PagedData<Shelf> = if (!showTabs || tabs.isEmpty())
            loadFeed(tabs.firstOrNull()).injectExtensionId(extension)
        else PagedData.Single {
            listOf(
                Shelf.Lists.Categories(
                    context.getString(R.string.tabs),
                    tabs.map {
                        Shelf.Category(
                            it.title,
                            loadFeed(it).injectExtensionId(extension)
                        )
                    },
                    type = Shelf.Lists.Type.Grid
                )
            )
        }
        shelf
    }

    override suspend fun getHomeTabs() = extensions().map { Tab(it.id, it.name) }
    override fun getHomeFeed(tab: Tab?): PagedData<Shelf> {
        val id = tab?.id ?: return PagedData.empty()
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<HomeFeedClient, PagedData<Shelf>> {
                feed(extension, { getHomeTabs() }, { getHomeFeed(it) })
            }
        }
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

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        return if (query.isBlank()) {
            getHistory().map { QuickSearchItem.Query(it, true) }
        } else listOf()
    }

    override suspend fun searchTabs(query: String) = extensions().map { Tab(it.id, it.name) }
    override fun searchFeed(query: String, tab: Tab?): PagedData<Shelf> {
        if (query.isNotBlank()) saveInHistory(query)

        val id = tab?.id ?: return PagedData.empty()
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<SearchFeedClient, PagedData<Shelf>> {
                feed(extension, { searchTabs(query) }, { searchFeed(query, it) })
            }
        }
    }

    override fun loadTracks(radio: Radio): PagedData<Track> {
        val id = radio.extras.extensionId
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<RadioClient, PagedData<Track>> {
                this.loadTracks(radio).injectExtension(extension)
            }
        }
    }

    override suspend fun radio(track: Track, context: EchoMediaItem?): Radio {
        val id = track.extras.extensionId
        return extensions().get(id).client<RadioClient, Radio> {
            radio(track, context).withExtensionId(id)
        }
    }

    override suspend fun radio(album: Album): Radio {
        val id = album.extras.extensionId
        return extensions().get(id).client<RadioClient, Radio> {
            radio(album).withExtensionId(id)
        }
    }

    override suspend fun radio(artist: Artist): Radio {
        val id = artist.extras.extensionId
        return extensions().get(id).client<RadioClient, Radio> {
            radio(artist).withExtensionId(id)
        }
    }

    override suspend fun radio(user: User): Radio {
        val id = user.extras.extensionId
        return extensions().get(id).client<RadioClient, Radio> {
            radio(user).withExtensionId(id)
        }
    }

    override suspend fun radio(playlist: Playlist): Radio {
        val id = playlist.extras.extensionId
        return extensions().get(id).client<RadioClient, Radio> {
            radio(playlist).withExtensionId(id)
        }
    }

    override suspend fun loadTrack(track: Track): Track {
        val id = track.extras.extensionId
        return extensions().get(id).client<TrackClient, Track> {
            loadTrack(track).withExtensionId(id)
        }.copy(isLiked = db.isLiked(track))
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable, isDownload: Boolean
    ): Streamable.Media {
        val id = streamable.extras.extensionId
        return extensions().get(id).client<TrackClient, Streamable.Media> {
            loadStreamableMedia(streamable, isDownload)
        }
    }

    override fun getShelves(track: Track): PagedData<Shelf> {
        val id = track.extras.extensionId
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<TrackClient, PagedData<Shelf>> {
                getShelves(track).injectExtensionId(extension)
            }
        }
    }

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val extId = playlist.extras.extensionId
        return if (extId == UNIFIED_ID) db.loadPlaylist(playlist)
        else extensions().get(extId).client<PlaylistClient, Playlist> {
            loadPlaylist(playlist).withExtensionId(extId)
        }
    }

    override fun loadTracks(playlist: Playlist): PagedData<Track> {
        val id = playlist.extras.extensionId
        return if (id == UNIFIED_ID) PagedData.Single { db.getTracks(playlist) }
        else PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<PlaylistClient, PagedData<Track>> {
                loadTracks(playlist).injectExtension(extension)
            }
        }
    }

    override fun getShelves(playlist: Playlist): PagedData<Shelf> {
        val id = playlist.extras.extensionId
        return if (id == UNIFIED_ID) PagedData.empty()
        else PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<PlaylistClient, PagedData<Shelf>> {
                getShelves(playlist).injectExtensionId(extension)
            }
        }
    }

    override suspend fun loadAlbum(album: Album): Album {
        val id = album.extras.extensionId
        return extensions().get(id).client<AlbumClient, Album> {
            loadAlbum(album).withExtensionId(id)
        }
    }

    override fun loadTracks(album: Album): PagedData<Track> {
        val id = album.extras.extensionId
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<AlbumClient, PagedData<Track>> {
                loadTracks(album).injectExtension(extension)
            }
        }
    }

    override fun getShelves(album: Album): PagedData<Shelf> {
        val id = album.extras.extensionId
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<AlbumClient, PagedData<Shelf>> {
                getShelves(album).injectExtensionId(extension)
            }
        }
    }

    override suspend fun loadUser(user: User): User {
        val id = user.extras.extensionId
        return extensions().get(id).client<UserClient, User> {
            loadUser(user).withExtensionId(id)
        }
    }

    override fun getShelves(user: User): PagedData<Shelf> {
        val id = user.extras.extensionId
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<UserClient, PagedData<Shelf>> {
                getShelves(user).injectExtensionId(extension)
            }
        }
    }

    override suspend fun loadArtist(artist: Artist): Artist {
        val id = artist.extras.extensionId
        return extensions().get(id).client<ArtistClient, Artist> {
            loadArtist(artist).withExtensionId(id)
        }
    }

    override fun getShelves(artist: Artist): PagedData<Shelf> {
        val id = artist.extras.extensionId
        return PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<ArtistClient, PagedData<Shelf>> {
                getShelves(artist).injectExtensionId(extension)
            }
        }
    }

    private val db = Room.databaseBuilder(
        context, UnifiedDatabase::class.java, "unified-database"
    ).fallbackToDestructiveMigration().build()

    override suspend fun getLibraryTabs() = listOf(
        Tab("Playlists", context.getString(R.string.playlists)),
        Tab("Saved", context.getString(R.string.saved))
    )

    override fun getLibraryFeed(tab: Tab?): PagedData<Shelf> {
        return when (tab?.id) {
            "Saved" -> PagedData.Single { db.getSaved().map { it.toShelf() } }
            else -> PagedData.Single { db.getPlaylists().map { it.toMediaItem().toShelf() } }
        }
    }

    override suspend fun saveToLibrary(mediaItem: EchoMediaItem, save: Boolean) {
        if (save) db.save(mediaItem) else db.deleteSaved(mediaItem)
    }

    override suspend fun isSavedToLibrary(mediaItem: EchoMediaItem): Boolean {
        return db.isSaved(mediaItem)
    }

    override suspend fun likeTrack(track: Track, isLiked: Boolean) {
        val likedPlaylist = db.getLikedPlaylist()
        val tracks = loadTracks(likedPlaylist).loadAll()
        if (isLiked) addTracksToPlaylist(likedPlaylist, tracks, 0, listOf(track))
        else removeTracksFromPlaylist(
            likedPlaylist,
            tracks,
            listOf(tracks.indexOfFirst { it.id == track.id })
        )
        val extension = extensions().get(track.extras.extensionId)
        extension.client<TrackLikeClient, Unit> { likeTrack(track, isLiked) }
    }

    override suspend fun listEditablePlaylists(): List<Playlist> {
        return db.getPlaylists()
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        return db.createPlaylist(title, description)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        db.deletePlaylist(playlist)
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?
    ) {
        db.editPlaylistMetadata(playlist, title, description)
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>
    ) {
        db.addTracksToPlaylist(playlist, tracks, index, new)
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist, tracks: List<Track>, indexes: List<Int>
    ) {
        db.removeTracksFromPlaylist(playlist, tracks, indexes)
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int
    ) {
        db.moveTrack(playlist, tracks, fromIndex, toIndex)
    }

    override fun searchTrackLyrics(clientId: String, track: Track) = PagedData.Suspend {
        val extId = track.extras.extensionId
        val extension = extensions().get(extId)
        runCatching {
            extension.client<LyricsClient, PagedData<Lyrics>> {
                searchTrackLyrics(clientId, track).injectLyricsExtId(extension)
            }
        }.getOrNull() ?: PagedData.empty()
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        val extId = lyrics.extras.extensionId
        return extensions().get(extId).client<LyricsClient, Lyrics> {
            loadLyrics(lyrics).withExtensionId(extId)
        }
    }
}