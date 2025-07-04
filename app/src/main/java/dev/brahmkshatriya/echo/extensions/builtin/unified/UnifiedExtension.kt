package dev.brahmkshatriya.echo.extensions.builtin.unified

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import dev.brahmkshatriya.echo.BuildConfig
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
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.clients.UserClient
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
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.playback.MediaItemUtils.toIdAndIndex
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

@OptIn(UnstableApi::class)
class UnifiedExtension(
    private val context: Context,
    private val downloadFeed: MutableStateFlow<List<Shelf>>,
    private val cache: SimpleCache?
) : ExtensionClient, MusicExtensionsProvider, HomeFeedClient, SearchFeedClient, LibraryFeedClient,
    PlaylistClient, AlbumClient, UserClient, ArtistClient, RadioClient, LyricsClient, TrackClient,
    TrackLikeClient, SaveToLibraryClient, PlaylistEditClient, TrackerClient, ShareClient {

    companion object {
        const val UNIFIED_ID = "unified"
        const val EXTENSION_ID = "extension_id"
        val metadata = Metadata(
            "UnifiedExtension",
            "",
            ImportType.BuiltIn,
            ExtensionType.MUSIC,
            UNIFIED_ID,
            "Unified Extension",
            version = "v${BuildConfig.VERSION_CODE}",
            "All your extensions in one place!",
            "Echo",
            isEnabled = true
        )

        suspend inline fun <reified C, T> Extension<*>.client(block: C.() -> T): T = runCatching {
            val client = instance.value().getOrThrow() as? C
                ?: throw ClientException.NotSupported(C::class.run { simpleName ?: java.name })
            client.block()
        }.getOrElse { throw it.toAppException(this) }

        suspend inline fun <reified C, T> Extension<*>.clientOrNull(block: C.() -> T): T? =
            runCatching {
                val client = instance.value().getOrThrow() as? C
                client?.block()
            }.getOrElse { throw it.toAppException(this) }

        private fun List<Extension<*>>.get(id: String?) =
            find { it.id == id } ?: throw Exception("Extension $id not found")

        private fun List<Extension<*>>.getOrNull(id: String?) = find { it.id == id }

        val Map<String, String>.extensionId
            get() = this[EXTENSION_ID] ?: throw Exception("Extension id not found")

        fun Track.withExtensionId(id: String, cached: Boolean = false) = copy(
            extras = extras + mapOf(EXTENSION_ID to id, "cached" to cached.toString()),
            album = album?.withExtensionId(id),
            artists = artists.map { it.withExtensionId(id) },
            streamables = streamables.map {
                it.copy(
                    extras = it.extras + mapOf(EXTENSION_ID to id, "cached" to cached.toString())
                )
            }
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

        fun Shelf.Item.withExtensionId(id: String) = copy(
            media = media.withExtensionId(id)
        )

        private fun Shelf.Category.withExtensionId(extension: Extension<*>): Shelf.Category =
            copy(items = items?.injectExtensionId(extension))

        private fun PagedData<Shelf.Category>.injectedPageCategory(extension: Extension<*>) =
            map { result ->
                val list = result.getOrElse { throw it.toAppException(extension) }
                list.map { it.withExtensionId(extension) }
            }

        private fun PagedData<EchoMediaItem>.injectedPageItems(extension: Extension<*>) =
            map { result ->
                val id = extension.id
                val list = result.getOrElse { throw it.toAppException(extension) }
                list.map { it.withExtensionId(id) }
            }

        private fun PagedData<Track>.injectedPageTracks(extension: Extension<*>) = map { result ->
            val id = extension.id
            val list = result.getOrElse { throw it.toAppException(extension) }
            list.map { it.withExtensionId(id) }
        }

        fun PagedData<Shelf>.injectExtensionId(extension: Extension<*>): PagedData<Shelf> =
            map { result ->
                val id = extension.id
                val list = result.getOrElse { throw it.toAppException(extension) }
                list.map {
                    when (it) {
                        is Shelf.Category -> it.withExtensionId(extension)
                        is Shelf.Item -> it.withExtensionId(id)
                        is Shelf.Lists.Categories -> it.copy(
                            list = it.list.map { category -> category.withExtensionId(extension) },
                            more = it.more?.injectedPageCategory(extension)
                        )

                        is Shelf.Lists.Items -> it.copy(
                            list = it.list.map { item -> item.withExtensionId(id) },
                            more = it.more?.injectedPageItems(extension)
                        )

                        is Shelf.Lists.Tracks -> it.copy(
                            list = it.list.map { track -> track.withExtensionId(id) },
                            more = it.more?.injectedPageTracks(extension)
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

        fun Tab.injectId(id: String) = copy(extras = extras + mapOf(EXTENSION_ID to id))
    }

    override val settingItems = listOf(
        SettingSwitch(
            context.getString(R.string.show_tabs),
            "show_tabs",
            context.getString(R.string.show_tab_summary),
            true
        ),
    )

    private lateinit var settings: Settings
    override fun setSettings(settings: Settings) {
        this.settings = settings
    }
    private val showTabs get() = settings.getBoolean("show_tabs") ?: true

    override val requiredMusicExtensions = listOf<String>()
    private var extFlow = MutableStateFlow<List<MusicExtension>?>(null)
    suspend fun extensions() = extFlow.first { it != null }!!
    override fun setMusicExtensions(extensions: List<MusicExtension>) {
        extFlow.value = extensions.filter { it.id != UNIFIED_ID && it.metadata.isEnabled }
    }

    private fun Extension<*>.loadFeed(
        extensions: List<Extension<*>>,
        loadTabs: suspend () -> List<Tab>,
        getFeed: (Tab?) -> PagedData<Shelf>
    ): PagedData<Shelf> = PagedData.Suspend {
        val tabs = runCatching { loadTabs() }.getOrElse { throw it.toAppException(this) }
        val loadFeed: (Tab?) -> PagedData<Shelf> =
            { runCatching { getFeed(it) }.getOrElse { throw it.toAppException(this) } }
        val shelf: PagedData<Shelf> =
            if (extensions.size == 1 || !showTabs || tabs.isEmpty()) loadFeed(tabs.firstOrNull()).injectExtensionId(
                this
            )
            else PagedData.Single {
                listOf(
                    Shelf.Lists.Categories(
                        context.getString(R.string.tabs), tabs.map {
                            Shelf.Category(
                                it.title, loadFeed(it).injectExtensionId(this)
                            )
                        }, type = Shelf.Lists.Type.Grid
                    )
                )
            }
        shelf
    }

    private suspend inline fun <reified T> tabs(
        loadTabs: T.() -> List<Tab>
    ): List<Tab> {
        val extensions = extensions()
        return if (extensions.size == 1) {
            val ext = extensions.first()
            ext.client<T, List<Tab>> { loadTabs() }.map { it.injectId(ext.id) }
        } else extensions.map { Tab(it.id, it.name).injectId(it.id) }
    }


    private inline fun <reified T : Any> feed(
        tab: Tab?,
        crossinline loadTabs: suspend T.() -> List<Tab>,
        crossinline getFeed: T.(Tab?) -> PagedData<Shelf>
    ): Feed {
        return PagedData.Suspend {
            val extensions = extensions()
            val id = tab?.extras?.extensionId ?: extensions.firstOrNull()?.id
            ?: return@Suspend PagedData.empty()
            val extension = extensions.get(id)
            extension.client<T, PagedData<Shelf>> {
                extension.loadFeed(extensions, { loadTabs() }, { getFeed(it) })
            }
        }.toFeed()
    }

    override suspend fun getHomeTabs() = tabs<HomeFeedClient> { getHomeTabs() }

    override fun getHomeFeed(tab: Tab?) =
        feed<HomeFeedClient>(tab, { getHomeTabs() }, { getHomeFeed(it).pagedData })

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        val history = getHistory().toMutableList()
        history.remove(item.title)
        context.saveToCache("search_history", history, "offline")
    }

    private fun getHistory() =
        context.getFromCache<List<String>>("search_history", "offline")?.distinct()?.take(5)
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

    override suspend fun searchTabs(query: String) = tabs<SearchFeedClient> { searchTabs(query) }
    override fun searchFeed(query: String, tab: Tab?): Feed {
        if (query.isNotBlank()) saveInHistory(query)
        return feed<SearchFeedClient>(
            tab,
            { searchTabs(query) },
            { searchFeed(query, it).pagedData })
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
        val cached = track.extras["cached"]?.toBoolean() ?: false
        if (cached) return track
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

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val extId = playlist.extras.extensionId
        return if (extId == UNIFIED_ID) {
            if (playlist.id == "cached") playlist
            else db.loadPlaylist(playlist)
        } else extensions().get(extId).client<PlaylistClient, Playlist> {
            loadPlaylist(playlist).withExtensionId(extId)
        }
    }

    override fun loadTracks(playlist: Playlist): PagedData<Track> {
        val id = playlist.extras.extensionId
        return if (id == UNIFIED_ID) PagedData.Single {
            if (playlist.id == "cached") cachedTracks
            else db.getTracks(playlist)
        }
        else PagedData.Suspend {
            val extension = extensions().get(id)
            extension.client<PlaylistClient, PagedData<Track>> {
                loadTracks(playlist).injectExtension(extension)
            }
        }
    }

    val db = Room.databaseBuilder(
        context, UnifiedDatabase::class.java, "unified-database"
    ).fallbackToDestructiveMigration(true).build()

    override suspend fun getLibraryTabs() = listOf(
        Tab("Unified", context.getString(R.string.all))
    ) + extensions().map { Tab(it.id, it.name) }

    private fun getCachedTracks() = cache?.keys?.mapNotNull { key ->
        val (id, _) = key.toIdAndIndex() ?: return@mapNotNull null
        context.getFromCache<Pair<String, Track>>(id.hashCode().toString(), "track")
    }?.reversed().orEmpty()
        .map { it.second.withExtensionId(it.first, true) }
        .groupBy { it.id }.map { it.value.first() }

    private var cachedTracks = listOf<Track>()
    override fun getLibraryFeed(tab: Tab?) = PagedData.Suspend {
        val extension = extensions().getOrNull(tab?.id)
        extension?.client<LibraryFeedClient, PagedData<Shelf>> {
            val tabs = getLibraryTabs()
            if (!showTabs || tabs.isEmpty())
                getLibraryFeed(tabs.firstOrNull()).pagedData.injectExtensionId(extension)
            else PagedData.Single {
                listOf(
                    Shelf.Lists.Categories(
                        context.getString(R.string.tabs), tabs.map {
                            Shelf.Category(
                                it.title, getLibraryFeed(it).pagedData.injectExtensionId(extension)
                            )
                        }, type = Shelf.Lists.Type.Grid
                    )
                )
            }
        } ?: PagedData.Single {
            cachedTracks = getCachedTracks()
            val cached = if (cachedTracks.isNotEmpty()) Playlist(
                id = "cached",
                title = context.getString(R.string.cached_songs),
                isEditable = false,
                cover = cachedTracks.first().cover,
                description = context.getString(R.string.cache_playlist_warning),
                tracks = cachedTracks.size,
                extras = mapOf(EXTENSION_ID to UNIFIED_ID)
            ).toMediaItem().toShelf() else null
            listOfNotNull(
                Shelf.Category(
                    context.getString(R.string.saved),
                    PagedData.Single { db.getSaved().map { it.toShelf() } }
                ),
                Shelf.Category(
                    context.getString(R.string.downloads),
                    PagedData.Single { downloadFeed.value }
                ),
                cached,
                *db.getCreatedPlaylists().map { it.toMediaItem().toShelf() }.toTypedArray()
            )
        }
    }.toFeed()

    override suspend fun saveToLibrary(mediaItem: EchoMediaItem, save: Boolean) {
        if (save) db.save(mediaItem) else db.deleteSaved(mediaItem)
    }

    override suspend fun isSavedToLibrary(mediaItem: EchoMediaItem): Boolean {
        return db.isSaved(mediaItem)
    }

    override suspend fun likeTrack(track: Track, isLiked: Boolean) {
        val likedPlaylist = db.getLikedPlaylist(context)
        val tracks = loadTracks(likedPlaylist).loadAll()
        if (isLiked) addTracksToPlaylist(likedPlaylist, tracks, 0, listOf(track))
        else removeTracksFromPlaylist(
            likedPlaylist, tracks, listOf(tracks.indexOfFirst { it.id == track.id })
        )
        val extension = extensions().get(track.extras.extensionId)
        extension.client<TrackLikeClient, Unit> { likeTrack(track, isLiked) }
    }

    override suspend fun listEditablePlaylists(track: Track?) = db.getCreatedPlaylists().map {
        val has = db.getTracks(it).any { t -> t.id == track?.id }
        it to has
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

    private var current: Track? = null
    override suspend fun onTrackChanged(details: TrackDetails?) {
        current = details?.track
        val id = details?.track?.extras?.extensionId ?: return
        val extension = extensions().get(id)
        extension.clientOrNull<TrackerClient, Unit> { onTrackChanged(details) }
    }

    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        val id = details.track.extras.extensionId
        val extension = extensions().get(id)
        extension.clientOrNull<TrackerClient, Unit> { onMarkAsPlayed(details) }
    }

    override suspend fun onPlayingStateChanged(details: TrackDetails?, isPlaying: Boolean) {
        val id = details?.track?.extras?.extensionId ?: return
        val extension = extensions().get(id)
        extension.clientOrNull<TrackerClient, Unit> { onPlayingStateChanged(details, isPlaying) }
    }

    override val markAsPlayedDuration: Long?
        get() {
            val extension = extFlow.value?.getOrNull(current?.extras?.extensionId) ?: return null
            return runCatching {
                extension.instance.value.run {
                    if (this !is TrackerClient) null
                    else markAsPlayedDuration
                }
            }.getOrElse { throw it.toAppException(extension) }
        }

    override suspend fun onShare(item: EchoMediaItem): String {
        val id = item.extras.extensionId
        val extension = extensions().get(id)
        return extension.client<ShareClient, String> { onShare(item) }
    }
}