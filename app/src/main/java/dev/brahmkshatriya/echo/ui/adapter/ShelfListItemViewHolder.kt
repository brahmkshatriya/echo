package dev.brahmkshatriya.echo.ui.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.playback.Current

abstract class ShelfListItemViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    abstract val transitionView: View
    abstract fun bind(item: Any)
    abstract fun onCurrentChanged(current: Current?)
    lateinit var adapter: ShelfListItemViewAdapter
    lateinit var shelf: Shelf.Lists<*>
}