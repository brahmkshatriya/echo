package dev.brahmkshatriya.echo.ui.bottom_player

import android.content.res.Configuration
import android.content.res.Resources.getSystem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import kotlin.math.min

class BottomPlayer(val activity: MainActivity, view: View, binding: BottomPlayerBinding) {

    init {

        val isLandscape =
            activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val collapsedCoverSize =
            activity.resources.getDimension(R.dimen.collapsed_cover_size).toInt()

        val maxCoverSize = with(activity.resources) {
            val size = getDimension(R.dimen.max_cover_size).toInt()
            val coverPadding = 24.dpToPx

            with(displayMetrics) {
                min(
                    (if (!isLandscape) widthPixels else heightPixels) - coverPadding * 2,
                    size
                )
            }
        }

        val coverHorizontalMargin = with(activity.resources.displayMetrics) {
            ((if (!isLandscape) widthPixels else widthPixels / 2) - maxCoverSize) / 2
        }

        val coverVerticalMargin = with(activity.resources.displayMetrics) {
            ((if (!isLandscape) widthPixels else heightPixels) - maxCoverSize) / 2
        }

        val collapsedDetailsContainerSize = with(activity.resources.displayMetrics) {
            widthPixels - collapsedCoverSize
        }

        val navView = activity.navView

        val bottomNavHeight = 140.dpToPx

        val bottomBehavior = BottomSheetBehavior.from(view)

        bottomBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                activity.viewModel.playerCollapsed.value =
                    (newState != STATE_COLLAPSED)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val inverseOffset = 1 - slideOffset

                binding.collapsedContainer.updateLayoutParams {
                    width = (collapsedDetailsContainerSize * inverseOffset).toInt()
                    binding.collapsedContainer.alpha = inverseOffset
                }

                binding.trackCover.updateLayoutParams<MarginLayoutParams> {
                    width =
                        (collapsedCoverSize + (maxCoverSize - collapsedCoverSize) * slideOffset).toInt()
                    height =
                        (collapsedCoverSize + (maxCoverSize - collapsedCoverSize) * slideOffset).toInt()
                    rightMargin = (coverHorizontalMargin * slideOffset).toInt()
                    leftMargin = (coverHorizontalMargin * slideOffset).toInt()
                    topMargin = (coverVerticalMargin * slideOffset).toInt()
                    bottomMargin = (coverVerticalMargin * slideOffset).toInt()
                }

                if (navView is BottomNavigationView)
                    navView.translationY = bottomNavHeight * slideOffset
                else
                    navView.translationX = -bottomNavHeight * slideOffset

            }
        })
        activity.viewModel.collapsePlayer = {
            bottomBehavior.state = STATE_COLLAPSED
        }

    }

    //    val Int.pxToDp: Float get() = (this / getSystem().displayMetrics.density)
    val Int.dpToPx: Int get() = (this * getSystem().displayMetrics.density).toInt()
}