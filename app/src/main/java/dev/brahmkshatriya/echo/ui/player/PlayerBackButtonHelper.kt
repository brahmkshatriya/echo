package dev.brahmkshatriya.echo.ui.player

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.ui.utils.observe
import kotlinx.coroutines.flow.MutableStateFlow

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
        viewLifecycleOwner.observe(playerCollapsed) {
            backPress.isEnabled = !it
            callback?.invoke(it)
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
