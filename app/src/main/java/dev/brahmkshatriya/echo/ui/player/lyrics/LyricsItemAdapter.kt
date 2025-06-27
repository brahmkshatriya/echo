package dev.brahmkshatriya.echo.ui.player.lyrics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.databinding.ItemLyricsItemBinding
import dev.brahmkshatriya.echo.databinding.SkeletonShelfBinding
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfEmptyAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfLoadingAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfLoadingAdapter.Companion.createListener

class LyricsItemAdapter(
    private val listener: Listener
) : PagingDataAdapter<Lyrics, LyricsItemAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Lyrics>() {
        override fun areItemsTheSame(oldItem: Lyrics, newItem: Lyrics) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Lyrics, newItem: Lyrics) = oldItem == newItem
    }

    fun interface Listener {
        fun onLyricsSelected(lyrics: Lyrics)
    }

    class ViewHolder(val binding: ItemLyricsItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLyricsItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lyrics = getItem(position) ?: return
        holder.binding.root.run {
            setOnClickListener { listener.onLyricsSelected(lyrics) }
            setTitle(lyrics.title)
            setSubtitle(lyrics.subtitle)
        }
    }

    data class Loading(
        val inflater: LayoutInflater,
        val parent: ViewGroup,
        val binding: SkeletonShelfBinding =
            SkeletonShelfBinding.inflate(inflater, parent, false)
    ) : ShelfLoadingAdapter.ViewHolder(binding.root)

    fun withLoaders(fragment: Fragment): ConcatAdapter {
        val listener = fragment.createListener { retry() }
        val footer = ShelfLoadingAdapter(::Loading, listener)
        val header = ShelfLoadingAdapter(::Loading, listener)
        val empty = ShelfEmptyAdapter()
        addLoadStateListener { loadStates ->
            empty.loadState = if (loadStates.refresh is LoadState.NotLoading && itemCount == 0)
                LoadState.Loading
            else LoadState.NotLoading(false)
            header.loadState = loadStates.refresh
            footer.loadState = loadStates.append
        }
        return ConcatAdapter(empty, header, this, footer)
    }

}