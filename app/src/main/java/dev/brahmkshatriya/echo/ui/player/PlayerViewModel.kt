package dev.brahmkshatriya.echo.ui.player

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    var collapsePlayer: (() -> Unit)? = null
    val playerCollapsed = MutableStateFlow(true)

    fun backPressedCallback(callback: ((Boolean) -> Unit)?): OnBackPressedCallback {
        val backPress = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                collapsePlayer?.invoke()
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

    companion object {
        fun handleBackPress(fragment: Fragment, callback: ((Boolean) -> Unit)? = null) =
            with(fragment) {
                val playerViewModel by activityViewModels<PlayerViewModel>()
                val backPressedCallback = playerViewModel.backPressedCallback(callback)
                requireActivity().onBackPressedDispatcher
                    .addCallback(viewLifecycleOwner, backPressedCallback)
            }

    }
}