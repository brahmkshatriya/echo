package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.utils.tryWith
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.radioNotSupported
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Radio(
    session: MediaLibrarySession,
    private val context: Context,
    private val settings: SharedPreferences,
    private val scope: CoroutineScope,
    private val extensionList: StateFlow<List<MusicExtension>?>,
    private val throwFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    private val stateFlow: MutableStateFlow<State>,
) : PlayerListener(session.player) {

    sealed class State {
        data object Empty : State()
        data object Loading : State()
        data class Loaded(
            val clientId: String, val playlist: Playlist, val tracks: List<Track>, val played: Int
        ) : State()
    }

    companion object {
        suspend fun start(
            context: Context,
            messageFlow: MutableSharedFlow<SnackBar.Message>,
            throwableFlow: MutableSharedFlow<Throwable>,
            stateFlow: MutableStateFlow<State>,
            extensionListFlow: StateFlow<List<MusicExtension>?>,
            clientId: String,
            item: EchoMediaItem,
            play: Int = -1
        ): State.Loaded? {
            val list = extensionListFlow.first { it != null }
            val extension = list?.find { it.metadata.id == clientId }
            when (val client = extension?.client) {
                null -> {
                    messageFlow.emit(context.noClient())
                }

                !is RadioClient -> {
                    messageFlow.emit(context.radioNotSupported(extension.metadata.name))
                }

                else -> {
                    stateFlow.value = State.Loading

                    suspend fun <T> tryIO(block: suspend () -> T): T? =
                        withContext(Dispatchers.IO) {
                            tryWith(throwableFlow) { block() }
                        }

                    val playlist = tryIO {
                        when (item) {
                            is TrackItem -> client.radio(item.track)
                            is Lists.PlaylistItem -> client.radio(item.playlist)
                            is Lists.AlbumItem -> client.radio(item.album)
                            is Profile.ArtistItem -> client.radio(item.artist)
                            is Profile.UserItem -> throw Exception("User radio not supported")
                        }
                    }

                    if (playlist != null) {
                        val tracks = tryIO { client.loadTracks(playlist).loadFirst() }
                        val state = if (!tracks.isNullOrEmpty()) State.Loaded(
                            clientId,
                            playlist,
                            tracks,
                            play
                        )
                        else {
                            messageFlow.emit(
                                SnackBar.Message(
                                    context.getString(R.string.radio_playlist_empty)
                                )
                            )
                            null
                        }
                        stateFlow.value = state ?: State.Empty
                        return state
                    } else {
                        stateFlow.value = State.Empty
                    }
                }
            }
            return null
        }
    }

    private fun play(loaded: State.Loaded, play: Int): Boolean {
        val track = loaded.tracks.getOrNull(play) ?: return false
        val item = MediaItemUtils.build(
            settings, track, loaded.clientId, loaded.playlist.toMediaItem()
        )
        player.addMediaItem(item)
        player.prepare()
        player.playWhenReady = true
        return true
    }

    private fun loadPlaylist() {
        val mediaItem = player.currentMediaItem ?: return
        val client = mediaItem.clientId
        val item = mediaItem.context ?: mediaItem.track.toMediaItem()
        scope.launch {
            val loaded = start(
                context, messageFlow, throwFlow, stateFlow, extensionList, client, item, 0
            ) ?: return@launch
            play(loaded, 0)
        }
    }

    override fun onTrackStart(mediaItem: MediaItem) {
        val autoStartRadio = settings.getBoolean(AUTO_START_RADIO, true)
        if (!autoStartRadio) return
        if (player.hasNextMediaItem()) return
        when (val state = stateFlow.value) {
            is State.Empty -> {
                if (stateFlow.value != State.Empty) return
                loadPlaylist()
            }

            State.Loading -> {}
            is State.Loaded -> {
                val toBePlayed = state.played + 1
                if (toBePlayed == state.tracks.size) loadPlaylist()
                if (play(state, toBePlayed)) {
                    stateFlow.value = state.copy(played = toBePlayed)
                }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        if(player.mediaItemCount == 0) stateFlow.value = State.Empty
    }
}

