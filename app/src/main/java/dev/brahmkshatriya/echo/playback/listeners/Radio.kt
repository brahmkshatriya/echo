package dev.brahmkshatriya.echo.playback.listeners

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.run
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.radioNotSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Radio(
    private val player: Player,
    private val context: Context,
    private val settings: SharedPreferences,
    private val scope: CoroutineScope,
    private val extensionList: StateFlow<List<MusicExtension>?>,
    private val throwFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<Message>,
    private val stateFlow: MutableStateFlow<State>,
) : Player.Listener {

    sealed class State {
        data object Empty : State()
        data object Loading : State()
        data class Loaded(
            val clientId: String, val radio: Radio, val tracks: List<Track>, val played: Int
        ) : State()
    }

    companion object {
        suspend fun start(
            context: Context,
            messageFlow: MutableSharedFlow<Message>,
            throwableFlow: MutableSharedFlow<Throwable>,
            extensionListFlow: StateFlow<List<MusicExtension>?>,
            clientId: String,
            item: EchoMediaItem,
            itemContext: EchoMediaItem?,
            play: Int = -1
        ): State.Loaded? {
            val list = extensionListFlow.first { it != null }
            val extension = list?.find { it.metadata.id == clientId }
            when (val client = extension?.instance?.value()?.getOrNull()) {
                null -> {
                    messageFlow.emit(context.noClient())
                }

                !is RadioClient -> {
                    messageFlow.emit(context.radioNotSupported(extension.name))
                }

                else -> {
                    suspend fun <T> tryIO(block: suspend () -> T): T? =
                        extension.run(throwableFlow) { block() }

                    val radio = tryIO {
                        when (item) {
                            is TrackItem -> client.radio(item.track, itemContext)
                            is Lists.PlaylistItem -> client.radio(item.playlist)
                            is Lists.AlbumItem -> client.radio(item.album)
                            is Profile.ArtistItem -> client.radio(item.artist)
                            is Profile.UserItem -> client.radio(item.user)
                            is Lists.RadioItem -> throw IllegalStateException("Radio inside radio")
                        }
                    }

                    if (radio != null) {
                        val tracks = tryIO { client.loadTracks(radio).loadFirst() }
                        val state = if (!tracks.isNullOrEmpty())
                            State.Loaded(clientId, radio, tracks, play)
                        else {
                            messageFlow.emit(
                                Message(context.getString(R.string.radio_empty))
                            )
                            null
                        }
                        return state
                    }
                }
            }
            return null
        }
    }

    private suspend fun play(loaded: State.Loaded, play: Int): Boolean =
        withContext(Dispatchers.Main) {
            val track = loaded.tracks.getOrNull(play) ?: return@withContext false
            val item = MediaItemUtils.build(
                settings, track, loaded.clientId, loaded.radio.toMediaItem()
            )
            player.addMediaItem(item)
            player.prepare()
            player.playWhenReady = true
            true
        }

    private suspend fun loadPlaylist() {
        val mediaItem = withContext(Dispatchers.Main) { player.currentMediaItem } ?: return
        val client = mediaItem.extensionId
        val item = mediaItem.track.toMediaItem()
        val itemContext = mediaItem.context
        stateFlow.value = State.Loading
        val loaded =
            start(context, messageFlow, throwFlow, extensionList, client, item, itemContext, 0)
        stateFlow.value = loaded ?: State.Empty
        if (loaded != null) play(loaded, 0)
    }

    private var autoStartRadio = settings.getBoolean(AUTO_START_RADIO, true)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
        if (key != AUTO_START_RADIO) return@OnSharedPreferenceChangeListener
        autoStartRadio = pref.getBoolean(AUTO_START_RADIO, true)
    }

    init {
        settings.registerOnSharedPreferenceChangeListener(listener)
    }

    private suspend fun startRadio() {
        if (!autoStartRadio) return
        val hasNext = withContext(Dispatchers.Main) {
            player.hasNextMediaItem() || player.currentMediaItem == null
        }
        if (hasNext) return
        when (val state = stateFlow.value) {
            is State.Loading -> {}
            is State.Empty -> loadPlaylist()
            is State.Loaded -> {
                val toBePlayed = state.played + 1
                if (toBePlayed == state.tracks.size) loadPlaylist()
                if (play(state, toBePlayed)) {
                    stateFlow.value = state.copy(played = toBePlayed)
                }
            }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        scope.launch { startRadio() }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (player.mediaItemCount == 0) stateFlow.value = State.Empty
        scope.launch { startRadio() }
    }
}

