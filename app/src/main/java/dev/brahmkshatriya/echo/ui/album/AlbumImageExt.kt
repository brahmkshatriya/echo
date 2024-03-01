package dev.brahmkshatriya.echo.ui.album

import android.view.View
import androidx.core.view.isVisible

fun albumImage(tracks: Int, view1: View, view2: View) {
    when (tracks) {
        1 -> {
            view1.isVisible = false
            view2.isVisible = false
        }
        2 -> {
            view1.isVisible = true
            view2.isVisible = false
        }
        else -> {
            view1.isVisible = true
            view2.isVisible = true
        }
    }
}