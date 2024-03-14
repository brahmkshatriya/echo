package dev.brahmkshatriya.echo.player

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionFlow
import dev.brahmkshatriya.echo.ui.settings.PreferenceFragment.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val global: Queue,
    trackFlow: ExtensionFlow,
    private val exceptionFlow: MutableSharedFlow<Exception>,
    private val settings: SharedPreferences,
    application: Application
) : ViewModel() {

    val fromNotification: MutableSharedFlow<Boolean> = MutableSharedFlow()

    private var trackClient: TrackClient? = null

    init {
        viewModelScope.observe(trackFlow.flow) {
            trackClient = it as? TrackClient
        }
    }

    val audioIndexFlow = MutableSharedFlow<Int>()
    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()
    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()
    val repeat: MutableSharedFlow<Int> = MutableSharedFlow()
    val shuffle: MutableSharedFlow<Boolean> = MutableSharedFlow()

    val clearQueueFlow = global.clearQueueFlow
    val addTrackFlow = global.addTrackFlow
    val moveTrackFlow = global.moveTrackFlow
    val removeTrackFlow = global.removeTrackFlow

    private suspend fun loadStreamable(track: Track): StreamableAudio? {
        return tryWith(exceptionFlow) { trackClient?.getStreamable(track) ?: throw trackException }
    }

    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        return stream?.let {
            global.addTrack(viewModelScope, track, it).first
        } ?: -1
    }

    fun play(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioIndexFlow.emit(loadAndAddToQueue(track))
        }
    }

    fun play(tracks: List<Track>, play: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            tracks.firstOrNull()?.run {
                val pos = loadAndAddToQueue(this)
                if (play) audioIndexFlow.emit(pos)
                tracks.drop(1).reversed().forEach {
                    loadAndAddToQueue(it)
                }
            }
        }
    }

    fun shuffle(shuffled: Boolean) {
        viewModelScope.launch {
            shuffle.emit(shuffled)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAndAddToQueue(track)
        }
    }

    fun clearQueue() {
        global.clearQueue(viewModelScope)
    }

    fun moveQueueItems(new: Int, old: Int) {
        global.moveTrack(viewModelScope, old, new)
    }

    fun removeQueueItem(index: Int) {
        global.removeTrack(viewModelScope, index)
    }

    fun radio(track: Track) {
        val autoStartRadio = settings.getBoolean(AUTO_START_RADIO, true)
        if(autoStartRadio) {
            val client = trackClient
            if (client is RadioClient) {
                viewModelScope.launch(Dispatchers.IO) {
                    val playlist =
                        tryWith(exceptionFlow) { client.radio(track) }
                            ?: return@launch
                    play(playlist.tracks, false)
                }
            }
        }
    }
    private val trackException = Exception(application.getString(R.string.is_not_supported, application.getString(R.string.track)))
    private val radioException = Exception(application.getString(R.string.is_not_supported, application.getString(R.string.radio)))
    fun radio(artist: Artist.Full){
        val client = trackClient
        if (client is RadioClient) {
            viewModelScope.launch(Dispatchers.IO) {
                val playlist = tryWith(exceptionFlow) { client.radio(artist) }
                    ?: return@launch
                play(playlist.tracks)
            }
        }
        else viewModelScope.launch { exceptionFlow.emit(radioException) }
    }

    fun radio(album: Album.Full){
        val client = trackClient
        if (client is RadioClient) {
            viewModelScope.launch(Dispatchers.IO) {
                val playlist = tryWith(exceptionFlow) { client.radio(album) }
                    ?: return@launch
                play(playlist.tracks)
            }
        }
        else viewModelScope.launch { exceptionFlow.emit(radioException) }
    }

//    fun radio(track: Track){
//        viewModelScope.launch(Dispatchers.IO) {
//            val playlist = radioClient.radio(track)
//            playlist.tracks.forEach {
//                audioQueueFlow.value = loadStreamable(it)
//            }
//        }
//    }

}