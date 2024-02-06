package dev.brahmkshatriya.echo.ui.player

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.brahmkshatriya.echo.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object PlayerBackButtonHelper {

    var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    val playerCollapsed = MutableStateFlow(true)
    private fun backPressedCallback(
        viewLifecycleOwner: LifecycleOwner,
        callback: ((Boolean) -> Unit)?
    ): OnBackPressedCallback {
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
        viewLifecycleOwner.lifecycleScope.launch {
            playerCollapsed.collectLatest {
                backPress.isEnabled = !it
                callback?.invoke(it)
            }
        }
        return backPress
    }

    fun addCallback(fragment: Fragment, callback: ((Boolean) -> Unit)? = null) =
        with(fragment) {
            val mainActivity = (requireActivity() as? MainActivity)
                ?: throw IllegalArgumentException("Fragment must be attached to MainActivity")
            val backPressedCallback =
                backPressedCallback(viewLifecycleOwner, callback)
            mainActivity.onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                backPressedCallback
            )
        }
}
