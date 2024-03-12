package dev.brahmkshatriya.echo.player.ui

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.player.Queue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class PlayerUIViewModel @Inject constructor(
    private val global: Queue
) : ViewModel() {
    var playlistTranslationY: Int = 0
    var bottomNavTranslateY: Int = 0
    var repeatMode: Int = 0

    val list
        get() = global.queue.mapIndexed { index, it -> (currentIndex.value == index) to it.second }

    fun getTrack(mediaId: String?) = global.getTrack(mediaId)
    private val _exceptionFlow = MutableSharedFlow<PlaybackException>()
    val exceptionFlow = _exceptionFlow

    fun createException(error: PlaybackException) {
        viewModelScope.launch {
            _exceptionFlow.emit(error)
        }
    }

    fun radio() {
        viewModelScope.launch {
            _radioFlow.emit(Unit)
        }
    }

    fun changeCurrent(index: Int?) {
        global.currentIndex.value = index
    }

    val track = MutableStateFlow(global.queue.firstOrNull()?.second)
    val currentIndex = global.currentIndex

    private val _radioFlow = MutableSharedFlow<Unit>()
    val radioFlow = _radioFlow.asSharedFlow()

    val progress = MutableStateFlow(0 to 0)
    val totalDuration = MutableStateFlow(0)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val shuffled = MutableStateFlow(false)

    val listChangeFlow = merge(
        global.addTrackFlow,
        global.removeTrackFlow,
        global.moveTrackFlow,
        global.clearQueueFlow,
        global.currentIndex,
    )
    var view: WeakReference<View> = WeakReference(null)
}