package dev.brahmkshatriya.echo.player.ui

import android.graphics.Color
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform


// We do not use this, cause it looks awful honestly
fun animatePlayerImage(
    fragment: Fragment,
    view: View,
) {
    val playerUIViewModel: PlayerUIViewModel by fragment.activityViewModels()
    val playerView = playerUIViewModel.view.get() ?: return
    val transform = MaterialContainerTransform().apply {
        startView = view
        duration = 1000
        endView = playerView
        endView?.let { addTarget(it) }
        pathMotion = MaterialArcMotion()
        scrimColor = Color.TRANSPARENT
    }
    TransitionManager.beginDelayedTransition(playerView.parent as ViewGroup, transform)
}