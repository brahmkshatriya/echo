package dev.brahmkshatriya.echo.ui.feed.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.feed.FeedType

sealed class FeedViewHolder<T : FeedType>(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(feed: T)
    open fun canBeSwiped(): Boolean = false
    open fun onSwipe(): T? = null
    open fun onCurrentChanged(current: PlayerState.Current?) {}
    var scrollAmount: Int = 0
}