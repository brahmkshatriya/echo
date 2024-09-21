package dev.brahmkshatriya.echo.offline

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
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
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.ImportType

class TestExtension : ExtensionClient, LoginClient.UsernamePassword, TrackClient, HomeFeedClient,
    RadioClient {

    companion object {
        val metadata = ExtensionMetadata(
            "TestExtension",
            "",
            ImportType.Inbuilt,
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
    override suspend fun loadTrack(track: Track) = track
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
        emptyList(),
        null,
        null,
        streamables = streamables
    ).toMediaItem().toShelf()

    override fun getHomeFeed(tab: Tab?): PagedData<Shelf> = PagedData.Single {
        listOf(
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
}