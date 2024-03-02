package dev.brahmkshatriya.echo.player.ui

import android.view.View
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.player.Queue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
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

    val track = MutableStateFlow(global.queue.firstOrNull()?.second)
    val currentIndex = MutableStateFlow<Int?>(null)

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
        currentIndex,
    )
    var view: WeakReference<View> = WeakReference(null)
}