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
import dev.brahmkshatriya.echo.common.models.Streamable.Audio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toAudioVideoMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toSubtitleMedia
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toVideoMedia
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
            null,
        )
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

    override suspend fun getStreamableMedia(streamable: Streamable): Streamable.Media {
        return when (streamable.mediaType) {
            Streamable.MediaType.Audio -> streamable.id.toAudio().toMedia()
            Streamable.MediaType.Video -> streamable.id.toVideoMedia()
            Streamable.MediaType.AudioVideo -> streamable.id.toAudioVideoMedia()
            Streamable.MediaType.Subtitle -> streamable.id.toSubtitleMedia(Streamable.SubtitleType.VTT)
        }
    }

    override fun getShelves(track: Track): PagedData<Shelf> = PagedData.Single {
        emptyList()
    }

    override suspend fun getHomeTabs() = listOf<Tab>()

    private val audio =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"

    private val video =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

    private val m3u8 =
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/adv_dv_atmos/main.m3u8"

    private val subtitle =
        "https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/MIB2-subtitles-pt-BR.vtt"

    private fun createTrack(id: String, title: String, streamables: List<Streamable>) = Track(
        id,
        title,
        isExplicit = Random.nextBoolean(),
        streamables = streamables
    ).toMediaItem().toShelf()

    override fun getHomeFeed(tab: Tab?): PagedData<Shelf> = PagedData.Single {
        listOf(
            Artist("bruh", "Bruh").toMediaItem().toShelf(),
            createTrack(
                "both", "All", listOf(
                    Streamable.audioVideo(audio, 0),
                    Streamable.audioVideo(video, 0),
                    Streamable.audioVideo(m3u8, 0, Streamable.MimeType.HLS),
                    Streamable.subtitle(subtitle)
                )
            ),
            createTrack("audio", "Audio", listOf(Streamable.audio(audio, 0))),
            createTrack("video", "Video", listOf(Streamable.video(video, 0))),
            createTrack(
                "audioVideo",
                "Audio Video",
                listOf(Streamable.audioVideo(audio, 0), Streamable.subtitle(subtitle))
            ),
            createTrack(
                "m3u8",
                "M3U8",
                listOf(
                    Streamable.audioVideo(m3u8, 0, Streamable.MimeType.HLS),
                    Streamable.subtitle(subtitle)
                )
            )
        )
    }

    private val radio = Radio("empty", "empty")
    override fun loadTracks(radio: Radio) = PagedData.Single<Track> { emptyList() }
    override suspend fun radio(track: Track, context: EchoMediaItem?) = radio
    override suspend fun radio(album: Album) = radio
    override suspend fun radio(artist: Artist) = radio
    override suspend fun radio(user: User) = radio
    override suspend fun radio(playlist: Playlist) = radio

    private var isFollowing = false
    override suspend fun followArtist(artist: Artist): Boolean {
        isFollowing = true
        println("follow")
        return true
    }

    override suspend fun unfollowArtist(artist: Artist): Boolean {
        isFollowing = false
        println("unfollow")
        return true
    }

    override suspend fun loadArtist(small: Artist): Artist {
        println("isFollowing : $isFollowing")
        return small.copy(isFollowing = isFollowing)
    }

    override fun getShelves(artist: Artist) = PagedData.Single<Shelf> {
        listOf(
            createTrack("audio", "Audio", listOf(Streamable.audio(audio, 0))),
        )
    }

    private var isSaved = false
    override suspend fun saveToLibrary(mediaItem: EchoMediaItem) {
        isSaved = true
        println("save")
    }

    override suspend fun removeFromLibrary(mediaItem: EchoMediaItem) {
        isSaved = false
        println("remove")
    }

    override suspend fun isSavedToLibrary(mediaItem: EchoMediaItem): Boolean {
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
        isHidden = isHidden
    )
}