package dev.brahmkshatriya.echo.ui.adapter

import android.view.View
import dev.brahmkshatriya.echo.common.models.Shelf

abstract class ShelfListItemViewHolder(itemView: View) :
    LifeCycleListAdapter.Holder<Any>(itemView) {
    abstract val transitionView: View
    lateinit var adapter: ShelfListItemViewAdapter
    lateinit var shelf: Shelf.Lists<*>
}