package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

class PlayerItemSpan(
    val context: Context,
    val client: String?,
    val item: EchoMediaItem,
    private val onItemClicked: (String?, EchoMediaItem) -> Unit
) : ClickableSpan() {

    override fun onClick(widget: View) {
        onItemClicked(client, item)
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
        context.run {
            ds.color = resources.getColor(R.color.button_player, theme)
        }
    }
}