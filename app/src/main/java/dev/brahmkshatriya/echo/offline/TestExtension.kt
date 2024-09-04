package dev.brahmkshatriya.echo.offline

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
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
    RadioClient, LibraryClient {

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

    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> = PagedData.Single {
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
    ).toMediaItem().toMediaItemsContainer()

    override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> = PagedData.Single {
        listOf(
            createTrack("audio", "Audio", listOf(Streamable.audio(audio, 0))),
            createTrack("video", "Video", listOf(Streamable.video(video, 0))),
            createTrack(
                "both", "Both", listOf(
                    Streamable.audio(audio, 0),
                    Streamable.video(video, 0),
                    Streamable.subtitle(subtitle)
                )
            ),
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

    private val emptyPlaylist = Playlist("empty", "empty", false)
    override suspend fun radio(track: Track) = emptyPlaylist
    override suspend fun radio(album: Album) = emptyPlaylist
    override suspend fun radio(artist: Artist) = emptyPlaylist
    override suspend fun radio(user: User) = emptyPlaylist
    override suspend fun radio(playlist: Playlist) = emptyPlaylist

    override suspend fun getLibraryTabs() = emptyList<Tab>()

    override fun getLibraryFeed(tab: Tab?): PagedData<MediaItemsContainer> {
        return PagedData.Single { emptyList() }
    }

    override suspend fun listEditablePlaylists(): List<Playlist> {
        return listOf(emptyPlaylist)
    }

    override suspend fun likeTrack(track: Track, liked: Boolean): Boolean {
        println("likeTrack: ${track.title}, $liked")
        return liked
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        return emptyPlaylist
    }

    override suspend fun deletePlaylist(playlist: Playlist) {}

    override suspend fun editPlaylistMetadata(
        playlist: Playlist,
        title: String,
        description: String?
    ) {
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        index: Int,
        new: List<Track>
    ) {
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>
    ) {
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int
    ) {
    }

    override suspend fun loadPlaylist(playlist: Playlist) = playlist
    override fun loadTracks(playlist: Playlist): PagedData<Track> = PagedData.Single {
        listOf(
            Track(
                "track",
                "Track",
                emptyList(),
                null,
                null,
                streamables = listOf(Streamable.audioVideo(video, 0))
            )
        )
    }

    override fun loadTracks(album: Album): PagedData<Track> {
        return PagedData.Single { emptyList() }
    }

    override fun getMediaItems(playlist: Playlist): PagedData<MediaItemsContainer> =
        PagedData.Single { listOf() }

    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> {
        return PagedData.Single { emptyList() }
    }

    override suspend fun loadAlbum(album: Album): Album {
        return album
    }
}