package dev.brahmkshatriya.echo.player.ui

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.MutableStateFlow

object PlayerBackButtonHelper {

    var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    var playlistBehavior: BottomSheetBehavior<View>? = null

    val playerSheetState = MutableStateFlow(STATE_HIDDEN)
    val playlistState = MutableStateFlow(STATE_COLLAPSED)
    private fun backPressedCallback(
        viewLifecycleOwner: LifecycleOwner,
        callback: ((Boolean) -> Unit)?
    ): OnBackPressedCallback {
        val backPress = object : OnBackPressedCallback(false) {
            fun getBehaviour() = playlistBehavior?.state?.let {
                if (it == STATE_EXPANDED) playlistBehavior
                else bottomSheetBehavior
            }

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                getBehaviour()?.startBackProgress(backEvent)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                getBehaviour()?.updateBackProgress(backEvent)
            }

            override fun handleOnBackPressed() {
                getBehaviour()?.handleBackInvoked()
            }

            override fun handleOnBackCancelled() {
                getBehaviour()?.cancelBackProgress()
            }
        }
        viewLifecycleOwner.observe(playerSheetState) {
            val expanded = it == STATE_EXPANDED
            backPress.isEnabled = expanded
            callback?.invoke(expanded)
        }
        return backPress
    }

    fun addCallback(fragment: Fragment, callback: ((Boolean) -> Unit)? = null) =
        with(fragment) {
            val mainActivity = (requireActivity() as? MainActivity)
                ?: throw IllegalArgumentException("Fragment must be attached to MainActivity")
            val backPressedCallback =
                backPressedCallback(viewLifecycleOwner, callback)
            mainActivity.onBackPressedDispatcher
                .addCallback(viewLifecycleOwner, backPressedCallback)
        }
}
