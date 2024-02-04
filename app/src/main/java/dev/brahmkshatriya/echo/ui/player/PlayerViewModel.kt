package dev.brahmkshatriya.echo.ui.player

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val offlineExtension: OfflineExtension
) : ViewModel() {
    var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    val playerCollapsed = MutableStateFlow(true)

    private fun backPressedCallback(callback: ((Boolean) -> Unit)?): OnBackPressedCallback {
        val backPress = object : OnBackPressedCallback(false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                bottomSheetBehavior?.startBackProgress(backEvent)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                bottomSheetBehavior?.updateBackProgress(backEvent)
            }

            override fun handleOnBackPressed() {
                bottomSheetBehavior?.handleBackInvoked()
            }

            override fun handleOnBackCancelled() {
                bottomSheetBehavior?.cancelBackProgress()
            }
        }
        viewModelScope.launch {
            playerCollapsed.collectLatest {
                backPress.isEnabled = !it
                callback?.invoke(it)
            }
        }
        return backPress
    }

    val audioFlow = MutableStateFlow<Pair<Track, StreamableAudio>?>(null)

    private fun loadStreamable(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioFlow.value = track to offlineExtension.getStreamable(track)
        }
    }

    fun play(track: Track) {
        loadStreamable(track)
    }

    fun handleBackPress(fragment: Fragment, callback: ((Boolean) -> Unit)? = null) =
        with(fragment) {
            val backPressedCallback = backPressedCallback(callback)
            requireActivity().onBackPressedDispatcher
                .addCallback(viewLifecycleOwner, backPressedCallback)
        }
}