package dev.brahmkshatriya.echo.ui.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

fun updatePaddingWithSystemInsets(view: View, bottom: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(insets.left, insets.top, insets.right, if (bottom) insets.bottom else 0)
        WindowInsetsCompat.CONSUMED
    }
}

fun updateBottomMarginWithSystemInsets(view: View, consumeInsets: Boolean = false) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = insets.bottom
        }
        if (consumeInsets) WindowInsetsCompat.CONSUMED else windowInsets
    }
}