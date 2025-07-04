package dev.brahmkshatriya.echo.extensions.builtin.test

import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LoginClient.InputField
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackHideClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.helpers.WebViewClient
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toBackgroundMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toSubtitleMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.providers.WebViewClientProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlin.random.Random

@Suppress("unused")
class TestExtension : ExtensionClient, LoginClient.CustomInput, TrackClient, LoginClient.WebView,
    HomeFeedClient, ArtistFollowClient, RadioClient, WebViewClientProvider,
    SaveToLibraryClient, TrackLikeClient, TrackHideClient, TrackerClient {

    companion object {
        val metadata = Metadata(
            "TestExtension",
            "",
            ImportType.BuiltIn,
            ExtensionType.MUSIC,
            "test",
            "Test Extension",
            "1.0.0",
            "Test extension for offline testing",
            "Test",
        )

        const val FUN =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"

        const val BUNNY =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        const val M3U8 =
            "https://fl3.moveonjoy.com/CNBC/index.m3u8"

        const val SUBTITLE =
            "https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/MIB2-subtitles-pt-BR.vtt"

        private fun createTrack(id: String, title: String, streamables: List<Streamable>) = Track(
            id,
            title,
            cover = "https://picsum.photos/480/270".toImageHolder(),
            isExplicit = Random.nextBoolean(),
            streamables = streamables
        ).toMediaItem().toShelf(true)
    }

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = emptyList()
    override fun setSettings(settings: Settings) {}

    override val forms = listOf(
        LoginClient.Form(
            "bruh",
            "Test Form",
            InputField.Type.Username,
            listOf(
                InputField(InputField.Type.Username, "name", "Name", true),
                InputField(InputField.Type.Password, "password", "Password", false),
                InputField(InputField.Type.Email, "email", "EMail", false),
                InputField(InputField.Type.Misc, "text", "Text", false),
                InputField(InputField.Type.Number, "number", "Number", false),
                InputField(InputField.Type.Url, "url", "Url", false),
            )
        )
    )

    override suspend fun onLogin(key: String, data: Map<String, String?>): List<User> {
        return listOf(
            User(
                key,
                "$key User",
                "https://picsum.photos/480/270".toImageHolder(),
            )
        )
    }

    override val webViewRequest = object : WebViewRequest.Cookie<List<User>> {
        override suspend fun onStop(url: Request, cookie: String): List<User> {
            return listOf(
                User(
                    "test_user",
                    "WebView User",
                    "https://picsum.photos/480/270".toImageHolder(),
                )
            )
        }

        override val initialUrl = "https://www.example.com/".toRequest()
        override val stopUrlRegex = "https://www\\.iana\\.org/.*".toRegex()
    }

    override suspend fun onSetLoginUser(user: User?) {
        println("setLoginUser: $user")
    }

    override suspend fun getCurrentUser(): User? = null


    override suspend fun loadStreamableMedia(
        streamable: Streamable, isDownload: Boolean
    ): Streamable.Media {
        if (streamable.quality == 3) throw Exception("Test exception for quality 3")
        return when (streamable.type) {
            Streamable.MediaType.Background -> streamable.id.toBackgroundMedia()
            Streamable.MediaType.Server -> {
                val srcs = Srcs.valueOf(streamable.id)
                when (srcs) {
                    Srcs.Single -> FUN.toServerMedia()
                    Srcs.Merged -> Streamable.Media.Server(
                        listOf(
                            BUNNY.toSource(),
                            FUN.toSource(),
                        ), false
                    )

                    Srcs.M3U8 -> M3U8.toServerMedia(type = Streamable.SourceType.HLS)
                }
            }

            Streamable.MediaType.Subtitle -> streamable.id.toSubtitleMedia(Streamable.SubtitleType.VTT)
        }
    }

    override fun getShelves(track: Track): PagedData<Shelf> = PagedData.Single {
        emptyList()
    }

    override suspend fun getHomeTabs() = listOf<Tab>()


    enum class Srcs {
        Single, Merged, M3U8;

        fun createTrack() = createTrack(
            name, name, listOf(Streamable.subtitle(SUBTITLE)) + (0..5).map { i ->
                Streamable.server(this.name, i)
            }
        )
    }

    private lateinit var webViewClient: WebViewClient
    override fun setWebViewClient(webViewClient: WebViewClient) {
        this.webViewClient = webViewClient
    }

    override fun getHomeFeed(tab: Tab?) = PagedData.Single {
        listOf(
            Shelf.Lists.Categories(
                "Bruh",
                listOf(
                    "Burhhhhhhh",
                    "brjdksls",
                    "sbajkxclllll",
                    "a",
                    "b",
                    " b"
                ).map { Shelf.Category(it, null) }
            ),
            Artist("bruh", "Bruh").toMediaItem().toShelf(),
            createTrack(
                "all", "All", listOf(
                    Streamable.server(Srcs.Single.name, 0),
                    Streamable.server(Srcs.Merged.name, 0),
                    Streamable.server(Srcs.M3U8.name, 0),
                    Streamable.subtitle(SUBTITLE)
                )
            ),
            Srcs.Single.createTrack(),
            Srcs.Merged.createTrack(),
            Srcs.M3U8.createTrack()
        )
    }.toFeed()

    private val radio = Radio("empty", "empty")
    override fun loadTracks(radio: Radio) = PagedData.Single {
        listOf(
            (Srcs.Merged.createTrack().media as EchoMediaItem.TrackItem).track,
        )
    }

    override suspend fun radio(track: Track, context: EchoMediaItem?) = radio
    override suspend fun radio(album: Album) = radio
    override suspend fun radio(artist: Artist) = radio
    override suspend fun radio(user: User) = radio
    override suspend fun radio(playlist: Playlist) = radio

    private var isFollowing = false
    override suspend fun followArtist(artist: Artist, follow: Boolean) {
        isFollowing = follow
        println("follow")
    }

    override suspend fun loadArtist(artist: Artist): Artist {
        println("isFollowing : $isFollowing")
        return artist.copy(isFollowing = isFollowing, extras = mapOf("loaded" to "Loaded bro"))
    }

    override fun getShelves(artist: Artist) = PagedData.Single<Shelf> {
        listOf(
            Srcs.Single.createTrack(),
            artist.toMediaItem().toShelf(),
        )
    }

    private var isSaved = false
    override suspend fun saveToLibrary(mediaItem: EchoMediaItem, save: Boolean) {
        println(mediaItem.extras["loaded"])
        isSaved = true
        println("save")
    }

    override suspend fun isSavedToLibrary(mediaItem: EchoMediaItem): Boolean {
        println(mediaItem.extras["loaded"])
        println("isSaved : $isSaved")
        return isSaved
    }

    private var isLiked = false
    override suspend fun likeTrack(track: Track, isLiked: Boolean) {
        this.isLiked = isLiked
        println("like")
    }

    private var isHidden = false
    override suspend fun hideTrack(track: Track, isHidden: Boolean) {
        println("hide")
        this.isHidden = isHidden
    }

    override suspend fun loadTrack(track: Track) = track.copy(
        isLiked = isLiked,
        isHidden = isHidden,
        extras = mapOf("loaded" to "Loaded bro")
    )

    override suspend fun onTrackChanged(details: TrackDetails?) {
        println("onTrackChanged : $details")
    }

    override suspend fun onPlayingStateChanged(details: TrackDetails?, isPlaying: Boolean) {
        println("onPlayingStateChanged $isPlaying : $details")
    }

    override val markAsPlayedDuration = 10000L
    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        println("onMarkAsPlayed : $details")
    }

}