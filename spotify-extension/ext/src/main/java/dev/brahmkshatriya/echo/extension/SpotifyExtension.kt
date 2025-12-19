package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.spotify.Queries
import dev.brahmkshatriya.echo.extension.spotify.SpotifyApi
import dev.brahmkshatriya.echo.extension.spotify.SpotifyApi.Companion.userAgent
import dev.brahmkshatriya.echo.extension.spotify.mercury.MercuryConnection
import dev.brahmkshatriya.echo.extension.spotify.mercury.StoredToken
import dev.brahmkshatriya.echo.extension.spotify.models.AccountAttributes
import dev.brahmkshatriya.echo.extension.spotify.models.ArtistOverview
import dev.brahmkshatriya.echo.extension.spotify.models.GetAlbum
import dev.brahmkshatriya.echo.extension.spotify.models.Item
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track.Format.MP4_128
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track.Format.MP4_256
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track.Format.OGG_VORBIS_160
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track.Format.OGG_VORBIS_320
import dev.brahmkshatriya.echo.extension.spotify.models.Metadata4Track.Format.OGG_VORBIS_96
import dev.brahmkshatriya.echo.extension.show
import dev.brahmkshatriya.echo.extension.spotify.models.UserProfileView
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Request
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

open class SpotifyExtension : ExtensionClient, LoginClient.WebView,
    SearchFeedClient, HomeFeedClient, LibraryFeedClient, LyricsClient, ShareClient,
    TrackClient, LikeClient, RadioClient, SaveClient,
    AlbumClient, PlaylistClient, ArtistClient, FollowClient, PlaylistEditClient {

    override suspend fun getSettingItems() = listOf(
        SettingSwitch(
            "Show Canvas",
            "show_canvas",
            "Whether to show background video canvas in songs",
            showCanvas
        ),
        SettingSwitch(
            "Crop Covers",
            "crop_covers",
            "Whether to crop artist and users images to fill the whole circle",
            cropCovers
        )
    )

    private val showCanvas get() = setting.getBoolean("show_canvas") ?: true
    private val cropCovers get() = setting.getBoolean("crop_covers") ?: true

    lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
    }

    open val filesDir = File("spotify")
    val api by lazy { SpotifyApi(filesDir) }
    val queries by lazy { Queries(api) }

    override val webViewRequest = object : WebViewRequest.Cookie<List<User>> {
        override val dontCache = true
        override val initialUrl =
            "https://accounts.spotify.com/en/login?allow_password=1".toGetRequest(mapOf(userAgent))
        override val stopUrlRegex =
            Regex("(https://accounts\\.spotify\\.com/.{2}/status.*)|(https://open\\.spotify\\.com)")

        val emailRegex = Regex("remember=([^;]+)")
        override suspend fun onStop(url: NetworkRequest, cookie: String): List<User> {
            if (!cookie.contains("sp_dc")) throw Exception("Token not found")
            val api = SpotifyApi(filesDir)
            api.setCookie(cookie)
            val email = emailRegex.find(cookie)?.groups?.get(1)?.value?.let {
                URLDecoder.decode(it, "UTF-8")
            }
            val user = Queries(api).profileAttributes().json.toUser().copy(
                extras = mapOf("cookie" to cookie),
                subtitle = email
            )
            return listOf(user)
        }
    }

    override fun setLoginUser(user: User?) {
        val cookie = if (user == null) null
        else user.extras["cookie"] ?: throw ClientException.Unauthorized(user.id)
        api.setCookie(cookie)
        api.setUser(user?.id)
        this.user = user
        this.product = null
    }

    private var user: User? = null
    override suspend fun getCurrentUser(): User? {
        return queries.profileAttributes().json.toUser()
    }

    private var product: AccountAttributes.Product? = null
    private suspend fun hasPremium(): Boolean {
        if (api.cookie == null) return false
        if (product == null) product = queries.accountAttributes().json.data.me.account.product
        return product != AccountAttributes.Product.FREE
    }

    private fun getBrowsePage(): Feed.Data<Shelf> = PagedData.Single {
        queries.browseAll().json.data.browseStart.sections.toShelves(queries, cropCovers)
    }.toFeedData()

    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        if (query.isBlank()) return Feed(listOf()) { getBrowsePage() }
        val (shelves, tabs) = queries.searchDesktop(query).json.data.searchV2
            .toShelvesAndTabs(query, queries, cropCovers)
        return Feed(tabs) { tab ->
            when (tab?.id) {
                "ARTISTS" -> paged {
                    queries.searchArtist(query, it).json.data.searchV2.artists
                        .toItemShelves(cropCovers)
                }

                "TRACKS" -> paged {
                    queries.searchTrack(query, it).json.data.searchV2.tracksV2
                        .toItemShelves(cropCovers)
                }

                "ALBUMS" -> paged {
                    queries.searchAlbum(query, it).json.data.searchV2.albumsV2
                        .toItemShelves(cropCovers)
                }

                "PLAYLISTS" -> paged {
                    queries.searchPlaylist(query, it).json.data.searchV2.playlists
                        .toItemShelves(cropCovers)
                }

                "GENRES" -> paged {
                    queries.searchGenres(query, it).json.data.searchV2.genres
                        .toCategoryShelves(queries, cropCovers)
                }

                "EPISODES" -> paged {
                    queries.searchFullEpisodes(query, it).json.data.searchV2.episodes
                        .toItemShelves(cropCovers)
                }

                "USERS" -> paged {
                    queries.searchUser(query, it).json.data.searchV2.users.toItemShelves(cropCovers)
                }

                else -> shelves
            }.toFeedData()
        }
    }

    override suspend fun loadFeed(track: Track) = PagedData.Single {
        val (union, rec) = coroutineScope {
            val a = async { queries.getTrack(track.id).json.data.trackUnion }
            val b = queries.internalLinkRecommenderTrack(track.id).json.data.seoRecommendedTrack
            a.await() to b
        }
        val list =
            rec.items.mapNotNull { it.data?.toTrack(cropCovers) }.takeIf { it.isNotEmpty() }?.let {
                listOf(Shelf.Lists.Tracks("${track.id}_more", "More like this", it))
            } ?: emptyList<Shelf>()
        val first =
            union.firstArtist?.items?.firstOrNull()?.toShelves(queries, cropCovers) ?: emptyList()
        val other =
            union.otherArtists?.items.orEmpty().map { it.toShelves(queries, cropCovers) }.flatten()
        list + first + other
    }.toFeed()

    override suspend fun loadStreamableMedia(
        streamable: Streamable, isDownload: Boolean,
    ): Streamable.Media {
        println("=== ECHO-SPOTIFY-v7 DEBUG: loadStreamableMedia called ===")
        println("DEBUG: streamable.id = ${streamable.id}")
        println("DEBUG: streamable.type = ${streamable.type}")
        println("DEBUG: streamable.extras = ${streamable.extras}")
        
        return when (streamable.type) {
            Streamable.MediaType.Server -> {
                api.cookie ?: throw ClientException.LoginRequired()
                val format = Metadata4Track.Format.valueOf(streamable.extras["format"]!!)
                println("DEBUG: format = $format")
                
                // Update streamable to use fileId from extras if needed
                val actualStreamable = if (streamable.id.startsWith("https://spotify-placeholder.local/")) {
                    streamable.copy(id = streamable.extras["fileId"]!!)
                } else {
                    streamable
                }
                
                return when (format) {
                    OGG_VORBIS_320, OGG_VORBIS_160, OGG_VORBIS_96 -> oggStream(actualStreamable)
                    MP4_256, MP4_128 -> widevineStream(actualStreamable)
                    else -> throw ClientException.NotSupported(format.name)
                }
            }

            Streamable.MediaType.Background ->
                Streamable.Media.Background(streamable.id.toGetRequest())

            else -> throw IllegalStateException("Unsupported Streamable : $streamable")
        }
    }

    open val showWidevineStreams = false

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track = coroutineScope {
        println("=== ECHO-SPOTIFY-v7 DEBUG: loadTrack started for ${track.id} ===")
        val hasPremium = hasPremium()
        val isLoggedIn = api.cookie != null
        if (!isLoggedIn) throw ClientException.LoginRequired()
        val canvas =
            if (showCanvas) async { queries.canvas(track.id).json.toStreamable() } else null
        
        val metadata = queries.metadata4Track(track.id)
        val metadataJson = metadata.json
        
        // Debug: Check what files are available from API
        val mainFiles = metadataJson.file ?: emptyList()
        val altFiles = metadataJson.alternative?.firstOrNull()?.file ?: emptyList()
        val allFiles = mainFiles.ifEmpty { altFiles }
        
        val availableFormats = allFiles.mapNotNull { it.format?.name }
        val hasFileId = allFiles.any { it.fileId != null }
        
        // Show which formats would pass the filter
        val passedFormats = allFiles.filter { file ->
            val format = file.format ?: return@filter false
            file.fileId != null && format.show(hasPremium, isLoggedIn, showWidevineStreams)
        }.mapNotNull { it.format?.name }
        
        val result = metadataJson.toTrack(
            hasPremium, isLoggedIn, showWidevineStreams, canvas?.await()
        ).copy(
            isExplicit = track.isExplicit
        )
        
        // Count audio streamables (exclude canvas/background)
        val audioStreamables = result.streamables.filter { it.type == Streamable.MediaType.Server }
        
        if (audioStreamables.isEmpty()) {
            val debugInfo = buildString {
                appendLine("=== ECHO-SPOTIFY-v5 DEBUG ===")
                appendLine("hasPremium=$hasPremium")
                appendLine("isLoggedIn=$isLoggedIn")
                appendLine("showWidevineStreams=$showWidevineStreams")
                appendLine("")
                appendLine("Files from API: ${allFiles.size}")
                appendLine("Available formats: $availableFormats")
                appendLine("Has fileId: $hasFileId")
                appendLine("Formats that passed filter: $passedFormats")
                appendLine("")
                appendLine("Track metadata gid: ${metadataJson.gid}")
                appendLine("Track name: ${metadataJson.name}")
                appendLine("")
                if (allFiles.isEmpty()) {
                    appendLine("ERROR: Spotify API returned NO audio files!")
                    appendLine("This could mean:")
                    appendLine("- Track is region-restricted")
                    appendLine("- Track requires premium")
                    appendLine("- API authentication issue")
                } else if (passedFormats.isEmpty()) {
                    appendLine("ERROR: No formats passed the filter!")
                    appendLine("All formats were filtered out by Format.show()")
                }
            }
            throw Exception(debugInfo)
        }
        result
    }

    private suspend fun createRadio(id: String): Radio {
        val radioId = queries.seedToPlaylist(id).json.mediaItems.first().uri
        return queries.fetchPlaylist(radioId).json.data.playlistV2.toRadio(cropCovers)!!
    }

    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?) = createRadio(item.id)
    override suspend fun loadRadio(radio: Radio) = radio
    override suspend fun loadTracks(radio: Radio) = loadPlaylistTracks(radio.id, true).toFeed()

    override suspend fun loadPlaylist(playlist: Playlist) =
        when (val type = playlist.id.substringAfter(":").substringBefore(":")) {
            "playlist" -> {
                val new =
                    queries.fetchPlaylist(playlist.id).json.data.playlistV2.toPlaylist(cropCovers)!!
                new.copy(
                    isEditable = runCatching {
                        val id = getPlaylistId(new)
                        queries.editPlaylistMetadata(id, new.title, new.description).json
                            .capabilities?.canEditItems
                    }.getOrNull() ?: false
                )
            }

            "collection" -> {
                val totalSongs = if (playlist.trackCount == null)
                    queries.fetchLibraryTracks(0).json.data.me.library.tracks.totalCount
                else playlist.trackCount

                likedPlaylist.copy(
                    cover = "https://misc.scdn.co/liked-songs/liked-songs-640.jpg".toImageHolder(),
                    trackCount = totalSongs
                )
            }

            else -> throw ClientException.NotSupported("Unsupported playlist type: $type")
        }

    private fun loadPlaylistTracks(id: String, skipFirst: Boolean = false) = paged { offset ->
        val content = queries.fetchPlaylistContent(id, offset).json.data.playlistV2.content!!
        val tracks = content.items!!.mapNotNull {
            val track = it.itemV2?.data?.toTrack(cropCovers, added = it.addedAt?.toDate())
                ?: return@mapNotNull null
            if (it.uid != null) track.copy(extras = mapOf("uid" to it.uid)) else track
        }.let {
            if (skipFirst && offset == 0) it.drop(1) else it
        }
        val page = content.pagingInfo!!
        val next = page.offset!! + page.limit!!
        tracks to if (content.totalCount!! > next) next else null
    }

    private fun loadLikedTracks() = paged { offset ->
        val content = queries.fetchLibraryTracks(offset).json.data.me.library.tracks
        val tracks = content.items.map {
            it.track.data?.toTrack(
                cropCovers,
                url = it.track.uri,
                added = it.addedAt?.toDate()
            )!!
        }
        val page = content.pagingInfo!!
        val next = page.offset!! + page.limit!!
        tracks to if (content.totalCount!! > next) next else null
    }

    override suspend fun loadTracks(playlist: Playlist) =
        when (val type = playlist.id.substringAfter(":").substringBefore(":")) {
            "playlist" -> loadPlaylistTracks(playlist.id).toFeed()
            "collection" -> loadLikedTracks().toFeed()
            else -> throw ClientException.NotSupported("Unsupported playlist type: $type")
        }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int,
    ) {
        getPlaylistId(playlist)
        val uid = tracks[fromIndex].extras["uid"]!!
        val before = if (fromIndex - toIndex > 0) 0 else 1
        val fromUid = tracks.getOrNull(toIndex + before)?.extras?.get("uid")
        queries.moveItemsInPlaylist(playlist.id, uid, fromUid)
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>,
    ) {
        if (api.cookie == null) throw ClientException.LoginRequired()
        when (val type = playlist.id.substringAfter(":").substringBefore(":")) {
            "playlist" -> {
                val uids = indexes.map { tracks[it].extras["uid"]!! }.toTypedArray()
                queries.removeFromPlaylist(playlist.id, *uids)
            }

            "collection" -> {
                val uris = indexes.map { tracks[it].id }.toTypedArray()
                queries.removeFromLibrary(*uris)
            }

            else -> throw ClientException.NotSupported("Unsupported playlist type: $type")
        }
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        index: Int,
        new: List<Track>,
    ) {
        if (api.cookie == null) throw ClientException.LoginRequired()
        when (val type = playlist.id.substringAfter(":").substringBefore(":")) {
            "playlist" -> {
                val uris = new.map { it.id }.toTypedArray()
                val fromUid = tracks.getOrNull(index)?.extras?.get("uid")
                queries.addToPlaylist(playlist.id, fromUid, *uris)
            }

            "collection" -> {
                val uris = new.map { it.id }.toTypedArray()
                queries.addToLibrary(*uris)
            }

            else -> throw ClientException.NotSupported("Unsupported playlist type: $type")
        }

    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        if (api.cookie == null) throw ClientException.LoginRequired()
        val uri = queries.createPlaylist(title, description).json.uri
        val userId = getCurrentUser()!!.id.substringAfter("spotify:user:")
        queries.playlistToLibrary(userId, uri)
        return loadPlaylist(Playlist(uri, title, true))
    }

    private fun getPlaylistId(playlist: Playlist): String {
        if (api.cookie == null) throw ClientException.LoginRequired()
        if (!playlist.id.startsWith("spotify:playlist:"))
            throw ClientException.NotSupported("Unsupported playlist type: ${playlist.id}")
        return playlist.id.substringAfter("spotify:playlist:")
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        getPlaylistId(playlist)
        val userId = getCurrentUser()!!.id.substringAfter("spotify:user:")
        queries.deletePlaylist(userId, playlist.id)
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist, title: String, description: String?,
    ) {
        val id = getPlaylistId(playlist)
        queries.editPlaylistMetadata(id, title, description).raw
    }

    override suspend fun listEditablePlaylists(track: Track?): List<Pair<Playlist, Boolean>> {
        return editablePlaylists(queries, track?.id, null, cropCovers).loadAll()
    }

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? =
        when (playlist.id.substringAfter(":").substringBefore(":")) {
            "playlist" -> {
                PagedData.Single<Shelf> {

                    val playlists = queries.seoRecommendedPlaylist(playlist.id).json.data
                        .seoRecommendedPlaylist.items.mapNotNull { it.toMediaItem(cropCovers) }
                    if (playlists.isEmpty()) return@Single emptyList()
                    listOf(
                        Shelf.Lists.Items(
                            id = "${playlist.id}_more",
                            title = "More Playlists like this",
                            list = playlists,
                        )
                    )
                }.toFeed()

            }

            else -> null
        }

    override suspend fun loadAlbum(album: Album): Album {
        val res = queries.getAlbum(album.id)
        return res.json.data.albumUnion.toAlbum(cropCovers)!!.copy(
            extras = mapOf("raw" to res.raw)
        )
    }


    override suspend fun loadFeed(album: Album): Feed<Shelf>? = when (album.type) {
        Album.Type.Show -> null
        Album.Type.Book -> null
        else -> PagedData.Single {
            val union = api.json.decode<GetAlbum>(album.extras["raw"]!!).data.albumUnion
            union.moreAlbumsByArtist?.items.orEmpty().map {
                it.toShelves(queries, cropCovers)
            }.flatten()
        }.toFeed()
    }

    override suspend fun loadTracks(album: Album) = when (album.type) {
        Album.Type.Show -> null
        Album.Type.Book -> null
        else -> {
            var next = 0L
            paged { offset ->
                val content =
                    queries.queryAlbumTracks(album.id, offset).json.data.albumUnion.tracksV2!!
                val tracks = content.items!!.map {
                    it.track?.toTrack(cropCovers, album)!!
                }
                next += tracks.size
                tracks to if (content.totalCount!! > next) next else null
            }.toFeed()
        }
    }

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        val home = queries.home(null).json.data?.home!!
        val tabs = if (api.cookie == null) emptyList() else
            listOf(Tab("", "All")) + home.homeChips?.toTabs()!!
        return Feed(tabs) { tab ->
            PagedData.Single {
                val res = if (tab == null || tab.id == "") home
                else queries.home(tab.id).json.data?.home!!
                res.toShelves(queries, cropCovers)
            }.toFeedData()
        }
    }

    override suspend fun onShare(item: EchoMediaItem): String {
        val type = item.id.substringAfter("spotify:").substringBefore(":")
        val id = item.id.substringAfter("spotify:$type:")
        return "https://open.spotify.com/$type/$id"
    }

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        if (api.cookie == null) throw ClientException.LoginRequired()
        val filters = queries.libraryV3(0).json.data?.me?.libraryV3?.availableFilters
            ?.mapNotNull { it.name }.orEmpty()
        val tabs = (listOf("All", "You") + filters).map { Tab(it, it) }
        return Feed(tabs) { tab ->
            when (tab?.id) {
                "All", null -> pagedLibrary(queries, cropCovers = cropCovers)
                "You" -> PagedData.Single {
                    val top = queries.userTopContent().json.data.me.profile

                    val id = getCurrentUser()!!.id.substringAfter("spotify:user:")
                    val uris = queries.recentlyPlayed(id).json.playContexts.map { it.uri }
                    listOfNotNull(
                        Shelf.Lists.Items(
                            "top_artists",
                            "Top Artist",
                            top.topArtists?.items.orEmpty().mapNotNull {
                                it.toMediaItem(cropCovers)
                            }
                        ),
                        Shelf.Lists.Tracks(
                            "top_tracks",
                            "Top Tracks",
                            top.topTracks?.items.orEmpty().mapNotNull {
                                (it.data as Item.Track).toTrack(cropCovers)
                            }
                        )
                    ) + queries.fetchEntitiesForRecentlyPlayed(uris).json.data.lookup.mapNotNull {
                        it.data?.toMediaItem(cropCovers)?.toShelf()
                    }
                }

                else -> pagedLibrary(queries, tab.id, null, cropCovers)
            }.toFeedData()
        }
    }

    override suspend fun saveToLibrary(item: EchoMediaItem, shouldSave: Boolean) {
        if (api.cookie == null) throw ClientException.LoginRequired()
        if (shouldSave) queries.addToLibrary(item.id)
        else queries.removeFromLibrary(item.id)
    }

    override suspend fun isItemSaved(item: EchoMediaItem): Boolean {
        if (api.cookie == null) return false
        val isSaved = queries.areEntitiesInLibrary(item.id)
            .json.data?.lookup?.firstOrNull()?.data?.saved
        return isSaved ?: false
    }

    override suspend fun likeItem(item: EchoMediaItem, shouldLike: Boolean) {
        if (shouldLike) queries.addToLibrary(item.id)
        else queries.removeFromLibrary(item.id)
    }

    override suspend fun isItemLiked(item: EchoMediaItem) = isItemSaved(item)

    override suspend fun isFollowing(item: EchoMediaItem) = isItemSaved(item)

    override suspend fun getFollowersCount(item: EchoMediaItem): Long? {
        if (item !is Artist) return null
        val type = item.id.substringAfter(":").substringBefore(":")
        val id = item.id.substringAfter("spotify:$type:")
        val req = Request.Builder().url("https://api.spotify.com/v1/${type}s/$id").build()
        val json = api.run { json.decode<JsonObject>(callGetBody(req)) }
        return json["followers"]?.jsonObject?.get("total")?.jsonPrimitive?.long
    }

    override suspend fun followItem(item: EchoMediaItem, shouldFollow: Boolean) {
        if (api.cookie == null) throw ClientException.LoginRequired()
        when (val type = item.id.substringAfter(":").substringBefore(":")) {
            "artist" -> {
                if (shouldFollow) queries.addToLibrary(item.id)
                else queries.removeFromLibrary(item.id)
            }

            "user" -> {
                val id = item.id.substringAfter("spotify:user:")
                if (shouldFollow) queries.followUsers(id)
                else queries.unfollowUsers(id)
            }

            else -> throw IllegalArgumentException("Unsupported artist type: $type")
        }
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return when (val type = artist.id.substringAfter(":").substringBefore(":")) {
            "artist" -> {
                val res = api.json.decode<ArtistOverview>(artist.extras["raw"]!!)
                res.data.artistUnion.toShelves(queries, cropCovers)
            }

            "user" -> {
                val res = api.json.decode<UserProfileView>(artist.extras["raw"]!!)
                val id = artist.id.substringAfter("spotify:user:")
                listOfNotNull(
                    res.toShelf(),
                    queries.profileFollowers(id).json.toShelf("${id}_followers", "Followers"),
                    queries.profileFollowing(id).json.toShelf("${id}_following", "Following")
                )
            }

            else -> throw IllegalArgumentException("Unsupported artist type: $type")
        }.toFeed(Feed.Buttons(showPlayAndShuffle = true))
    }

    override suspend fun loadArtist(artist: Artist): Artist {
        when (val type = artist.id.substringAfter(":").substringBefore(":")) {
            "artist" -> {
                val res = queries.queryArtistOverview(artist.id)
                return res.json.data.artistUnion.toArtist(null, cropCovers)!!.copy(
                    extras = mapOf("raw" to res.raw)
                )
            }

            "user" -> {
                val id = artist.id.substringAfter("spotify:user:")
                val profile = queries.profileWithPlaylists(id)
                return profile.json.toArtist()!!.copy(
                    extras = mapOf("raw" to profile.raw)
                )
            }

            else -> throw IllegalArgumentException("Unsupported artist type: $type")
        }
    }

    override suspend fun searchTrackLyrics(clientId: String, track: Track) = PagedData.Single {
        val id = track.id.substringAfter("spotify:track:")
        val image = track.cover as ImageHolder.NetworkRequestImageHolder
        val lyrics = runCatching { queries.colorLyrics(id, image.request.url).json.lyrics }
            .getOrNull() ?: return@Single emptyList<Lyrics>()
        var last = Long.MAX_VALUE
        val list = lyrics.lines?.reversed()?.mapNotNull {
            val start = it.startTimeMs?.toLong()!!
            val item = Lyrics.Item(
                it.words ?: return@mapNotNull null,
                startTime = start,
                endTime = last,
            )
            last = start
            item
        }?.reversed() ?: return@Single emptyList<Lyrics>()
        listOf(
            Lyrics(
                id = track.id,
                title = track.title,
                subtitle = lyrics.providerDisplayName,
                lyrics = Lyrics.Timed(list)
            )
        )
    }.toFeed()

    override suspend fun loadLyrics(lyrics: Lyrics) = lyrics

    private suspend fun widevineStream(streamable: Streamable): Streamable.Media.Server {
        val accessToken = api.getWebAccessToken()
        val url = queries.storageResolve(streamable.id).json.cdnUrl.first()
        val time = "time=${System.currentTimeMillis()}"
        val decryption = Streamable.Decryption.Widevine(
            "https://spclient.wg.spotify.com/widevine-license/v1/audio/license?$time"
                .toGetRequest(
                    mapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Origin" to "https://open.spotify.com",
                    )
                ),
            true
        )
        return Streamable.Source.Http(
            request = url.toGetRequest(),
            decryption = decryption,
        ).toMedia()
    }

    val time = 3000L
    var lastFetched = 0L
    val mutex = Mutex()

    open suspend fun getKey(accessToken: String, fileId: String): ByteArray =
        throw IllegalStateException()

    private suspend fun oggStream(streamable: Streamable): Streamable.Media {
        println("=== ECHO-SPOTIFY-v7 DEBUG: oggStream started ===")
        val fileId = streamable.id
        val gid = streamable.extras["gid"]
            ?: throw IllegalArgumentException("GID is required for streaming")

        // Get the audio key with retry logic
        val key = mutex.withLock {
            val lastTime = System.currentTimeMillis() - lastFetched
            if (lastTime < time) delay(time - lastTime)
            val storedToken = api.getMercuryToken()
            lastFetched = System.currentTimeMillis()
            
            // Retry up to 3 times on failure
            var lastError: Exception? = null
            for (attempt in 1..3) {
                try {
                    println("DEBUG: Fetching audio key (attempt $attempt)")
                    val key = MercuryConnection.getAudioKey(storedToken, gid, fileId)
                    println("DEBUG: Audio key fetched successfully")
                    return@withLock key
                } catch (e: Exception) {
                    println("DEBUG: Failed to fetch audio key (attempt $attempt): ${e.message}")
                    lastError = e
                    if (attempt < 3) {
                        delay(1000L * attempt) // Exponential backoff
                    }
                }
            }
            throw lastError ?: Exception("Failed to get audio key after 3 attempts")
        }
        
        // Get CDN URL
        val cdnUrls = queries.storageResolve(streamable.id).json.cdnUrl
        println("DEBUG: Found ${cdnUrls.size} CDN URLs")
        var urlIndex = 0
        
        return Streamable.InputProvider { position, length ->
            println("DEBUG: Reading stream pos=$position len=$length")
            decryptFromPosition(key, AUDIO_IV, position, length) { pos, len ->
                val range = "bytes=$pos-${len?.toString() ?: ""}"
                
                // Try multiple CDN URLs on failure
                var lastError: Exception? = null
                for (i in cdnUrls.indices) {
                    val url = cdnUrls[(urlIndex + i) % cdnUrls.size]
                    try {
                        println("DEBUG: Connecting to CDN: $url (Range: $range)")
                        val request = Request.Builder().url(url)
                            .header("Range", range)
                            .build()
                        val resp = api.client.newCall(request).await()
                        if (resp.isSuccessful) {
                            println("DEBUG: CDN connection successful")
                            val actualLength = resp.header("Content-Length")?.toLong() ?: -1L
                            return@decryptFromPosition resp.body.byteStream() to actualLength
                        }
                        println("DEBUG: CDN connection failed: ${resp.code}")
                        resp.close()
                    } catch (e: Exception) {
                        println("DEBUG: CDN connection exception: ${e.message}")
                        lastError = e
                        urlIndex = (urlIndex + 1) % cdnUrls.size
                    }
                }
                throw lastError ?: Exception("All CDN URLs failed")
            }
        }.toSource(fileId).toMedia()
    }

    private suspend fun decryptFromPosition(
        key: ByteArray,
        iv: BigInteger,
        position: Long,
        length: Long,
        provider: suspend (Long, Long?) -> Pair<InputStream, Long>,
    ): Pair<InputStream, Long> {
        val newPos = position + 0xA7
        val alignedPos = newPos - (newPos % 16)
        val blockOffset = (newPos % 16).toInt()
        val len = if (length < 0) null else length + newPos - 1
        val (input, contentLength) = provider(alignedPos, len)

        val ivCounter = iv.add(BigInteger.valueOf(alignedPos / 16))
        val ivBytes = ivCounter.to16ByteArray()

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            IvParameterSpec(ivBytes)
        )

        val cipherStream = CipherInputStream(input, cipher)

        cipherStream.skipBytes(blockOffset)
        return cipherStream to contentLength - blockOffset
    }

    companion object {
        private val AUDIO_IV = BigInteger("72e067fbddcbcf77ebe8bc643f630d93", 16)
        private fun BigInteger.to16ByteArray(): ByteArray {
            val full = toByteArray()
            return when {
                full.size == 16 -> full
                full.size > 16 -> full.copyOfRange(full.size - 16, full.size)
                else -> ByteArray(16 - full.size) + full
            }
        }

        fun InputStream.skipBytes(len: Int) {
            var remaining = len
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (remaining > 0) {
                val toRead = minOf(remaining, buffer.size)
                val read = read(buffer, 0, toRead)
                if (read == -1) break // EOF
                remaining -= read
            }
            if (remaining > 0) throw EOFException("Reached end of stream before reading $len bytes")
        }
    }
}