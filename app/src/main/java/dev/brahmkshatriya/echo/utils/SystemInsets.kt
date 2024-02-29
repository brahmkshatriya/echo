package dev.brahmkshatriya.echo.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior


fun View.updatePaddingWithPlayerAndSystemInsets(playerState: Int, top: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = insets.left,
            top = if(top) insets.top else 0,
            right = insets.right,
            bottom = if (playerState != BottomSheetBehavior.STATE_HIDDEN)
                insets.bottom + 88.dpToPx()
            else insets.bottom + 8.dpToPx()
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}