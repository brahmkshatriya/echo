package dev.brahmkshatriya.echo.ui.player

import android.content.res.Resources.getSystem
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets

class PlayerView(activity: MainActivity, view: View, binding: BottomPlayerBinding) {

    init {

        updatePaddingWithSystemInsets(binding.expandedContainer, false)
        view.setOnClickListener {
            BottomSheetBehavior.from(view).state = STATE_EXPANDED
        }

        val bottomBehavior = BottomSheetBehavior.from(view)
        val viewModel by activity.viewModels<PlayerViewModel>()

        val navView = activity.navView
        val bottomNavHeight = 140.dpToPx
        val collapsedCoverSize =
            activity.resources.getDimension(R.dimen.collapsed_cover_size).toInt()

        bottomBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                viewModel.playerCollapsed.value = (newState == STATE_COLLAPSED)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.collapsedContainer.translationY = -collapsedCoverSize * slideOffset
                binding.expandedContainer.translationY = collapsedCoverSize * (1 - slideOffset)

                if (navView is BottomNavigationView)
                    navView.translationY = bottomNavHeight * slideOffset
                else
                    navView.translationX = -bottomNavHeight * slideOffset
            }
        })

        viewModel.collapsePlayer = {
            bottomBehavior.state = STATE_COLLAPSED
        }

        view.post {
            bottomBehavior.state = viewModel.playerCollapsed.value.let {
                if (it) STATE_COLLAPSED else STATE_EXPANDED
            }
        }
    }

    private val Int.dpToPx: Int get() = (this * getSystem().displayMetrics.density).toInt()
}