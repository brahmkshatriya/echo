package dev.brahmkshatriya.echo.player.ui

import android.view.View
import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.player.Global
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference

class PlayerUIViewModel : ViewModel() {
    var playlistTranslationY: Int = 0
    var bottomNavTranslateY: Int = 0
    var repeatMode: Int = 0

    val track = MutableStateFlow(Global.queue.firstOrNull()?.second)
    val playlist = MutableStateFlow<Int?>(null)

    val progress = MutableStateFlow(0 to 0)
    val totalDuration = MutableStateFlow(0)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val shuffled = MutableStateFlow(false)

    var view: WeakReference<View> = WeakReference(null)
}