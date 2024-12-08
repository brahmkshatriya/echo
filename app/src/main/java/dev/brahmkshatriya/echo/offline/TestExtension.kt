package dev.brahmkshatriya.echo.offline

import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackHideClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toBackgroundMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toSubtitleMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlin.random.Random

class TestExtension : ExtensionClient, LoginClient.UsernamePassword, TrackClient, HomeFeedClient,
    ArtistFollowClient, RadioClient, SaveToLibraryClient, TrackLikeClient, TrackHideClient {

    companion object {
        val metadata = Metadata(
            "TestExtension",
            "",
            ImportType.BuiltIn,
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
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/adv_dv_atmos/main.m3u8"

        const val SUBTITLE =
            "https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/MIB2-subtitles-pt-BR.vtt"

        private fun createTrack(id: String, title: String, streamables: List<Streamable>) = Track(
            id,
            title,
            isExplicit = Random.nextBoolean(),
            streamables = streamables
        ).toMediaItem().toShelf()
    }

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = emptyList()
    override fun setSettings(settings: Settings) {}
    override suspend fun onLogin(username: String, password: String): List<User> {
        return listOf(User(username, username, null))
    }

    override suspend fun onSetLoginUser(user: User?) {
        println("onSetLoginUser: $user")
    }

    override suspend fun getCurrentUser(): User? = null


    override suspend fun loadStreamableMedia(streamable: Streamable): Streamable.Media {
        return when (streamable.type) {
            Streamable.MediaType.Background -> streamable.id.toBackgroundMedia()
            Streamable.MediaType.Server -> {
                val srcs = Srcs.valueOf(streamable.id)
                when (srcs) {
                    Srcs.Single -> throw Exception("Single source not supported")
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
            name, name, listOf(Streamable.server(this.name, 0), Streamable.subtitle(SUBTITLE))
        )
    }

    override fun getHomeFeed(tab: Tab?): PagedData<Shelf> = PagedData.Single {
        listOf(
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
    }

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
}