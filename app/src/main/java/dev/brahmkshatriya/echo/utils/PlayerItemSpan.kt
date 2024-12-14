package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import dev.brahmkshatriya.echo.R

class PlayerItemSpan<T>(
    val context: Context,
    val item: T,
    private val onItemClicked: (T) -> Unit
) : ClickableSpan() {

    override fun onClick(widget: View) {
        onItemClicked(item)
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
        context.run {
            ds.color = resources.getColor(R.color.button_player, theme)
        }
    }
}