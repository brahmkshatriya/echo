package dev.brahmkshatriya.echo.utils.ui.scrolling

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class ScrollAnimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var scrollAmount: Int = 0
}