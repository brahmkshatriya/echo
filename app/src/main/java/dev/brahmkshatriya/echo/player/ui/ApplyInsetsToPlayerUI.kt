package dev.brahmkshatriya.echo.player.ui

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R

fun applyInsetsToPlayerUI(
    activity: MainActivity
) {

    val playerBinding = activity.binding.bottomPlayer
    val playlistBinding = playerBinding.bottomPlaylist

    val container = activity.binding.bottomPlayerContainer as View
    val playlistContainer = playerBinding.bottomPlaylistContainer as View

    val uiViewModel: PlayerUIViewModel by activity.viewModels()

    val bottomPlayerBehavior = BottomSheetBehavior.from(container)
    val playlistBehavior = BottomSheetBehavior.from(playlistContainer)

    val peekHeight = activity.resources.getDimension(R.dimen.bottom_player_peek_height).toInt()
    val playlistPeekHeight = activity.resources.getDimension(R.dimen.playlist_peek_height).toInt()

    val navView = activity.binding.navView

    val orientation: Int = activity.resources.configuration.orientation

    ViewCompat.setOnApplyWindowInsetsListener(activity.binding.root) { _, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        bottomPlayerBehavior.peekHeight = peekHeight + systemInsets.bottom
        playlistBehavior.peekHeight = playlistPeekHeight + systemInsets.bottom
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            navView.post {
                uiViewModel.bottomNavTranslateY = navView.height
            }
        } else {
            navView.post {
                uiViewModel.bottomNavTranslateY = -navView.height
            }
        }
        activity.binding.snackbarContainer.updateLayoutParams {
            height = systemInsets.bottom
        }
        insets
    }

    if (orientation != Configuration.ORIENTATION_PORTRAIT) {

        // Need to manually handle system insets for landscape mode
        // since we can't use the fitSystemWindows on the root view,
        // or the playlist bottom sheet will be hidden behind the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(
            playerBinding.expandedTrackCoverContainer
        ) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updateLayoutParams<MarginLayoutParams> {
                topMargin = systemInsets.top
                bottomMargin = systemInsets.bottom
                leftMargin = systemInsets.left
            }

            playerBinding.collapsedContainer.updateLayoutParams<MarginLayoutParams> {
                leftMargin = systemInsets.left
                rightMargin = systemInsets.right
            }

            playerBinding.collapsePlayer.updateLayoutParams<MarginLayoutParams> {
                topMargin = systemInsets.top
            }

            playerBinding.expandedTrackInfoContainer.updatePadding(
                top = systemInsets.top,
                bottom = systemInsets.bottom
            )

            playerBinding.coordinatorLayout.updatePadding(
                right = systemInsets.right
            )

            playerBinding.bottomPlaylistContainer.updatePadding(
                left = 0,
                right = 0,
                bottom = systemInsets.bottom
            )

            uiViewModel.bottomNavTranslateY = -(navView.height + systemInsets.top)
            insets
        }
    }


    ViewCompat.setOnApplyWindowInsetsListener(playlistBinding.root) { _, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        uiViewModel.playlistTranslationY = systemInsets.top
        playlistBinding.root.translationY = -uiViewModel.playlistTranslationY.toFloat()
        insets
    }
}
