package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfListsBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.ShelfListsAdapter
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import kotlin.math.ceil
import kotlin.math.roundToInt

class ListsShelfViewHolder(
    viewPool: RecycledViewPool,
    private val listener: ShelfAdapter.Listener,
    private val binding: ItemShelfListsBinding
) : ShelfAdapter.ViewHolder(binding.root) {

    private val adapter = ShelfListsAdapter(listener)
    private var shelf: Shelf.Lists<*>? = null

    override var scrollAmount: Int
        get() = super.scrollAmount
        set(value) {
            adapter.scrollAmountY = value
            super.scrollAmount = value
        }

    init {
        binding.more.setOnClickListener {
            listener.onMoreClicked(extensionId, shelf, binding.titleCard)
        }
        binding.shuffle.setOnClickListener {
            listener.onShuffleClicked(extensionId, shelf as? Shelf.Lists.Tracks, binding.titleCard)
        }
        binding.recyclerView.setRecycledViewPool(viewPool)
        binding.recyclerView.adapter = adapter
    }

    val layoutManager get() = binding.recyclerView.layoutManager

    override fun bind(item: Shelf?) {
        binding.root.applyTranslationYAnimation(scrollAmount)
        val shelf = item as? Shelf.Lists<*> ?: return
        this.shelf = shelf
        binding.title.text = shelf.title
        binding.title.isVisible = shelf.title.isNotBlank()
        binding.subtitle.text = shelf.subtitle
        binding.subtitle.isVisible = !shelf.subtitle.isNullOrBlank()
        binding.more.isVisible = shelf.more != null
        binding.shuffle.isVisible = when (shelf) {
            is Shelf.Lists.Categories -> false
            is Shelf.Lists.Items -> false
            is Shelf.Lists.Tracks -> true
        }
        val layoutManager = binding.recyclerView.context.getLayoutManager(shelf)
        binding.recyclerView.layoutManager = layoutManager
        val horizontalPadding =
            if (shelf.type == Shelf.Lists.Type.Linear && shelf is Shelf.Lists.Tracks) 0
            else 20.dpToPx(binding.recyclerView.context)
        binding.recyclerView.updatePaddingRelative(
            start = horizontalPadding, end = horizontalPadding
        )
        layoutManager.initialPrefetchItemCount = binding.recyclerView.getPrefetchCount(item)

//        val transition = transitionView.transitionName + item.id
//        shelfAdapter.transition = transition
        binding.recyclerView.layoutManager = layoutManager
        adapter.submit(extensionId, shelf)
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        adapter.onCurrentChanged(current)
    }

    companion object {
        fun create(
            viewPool: RecycledViewPool,
            listener: ShelfAdapter.Listener,
            inflater: LayoutInflater,
            parent: ViewGroup
        ): ListsShelfViewHolder {
            val binding = ItemShelfListsBinding.inflate(inflater, parent, false)
            return ListsShelfViewHolder(viewPool, listener, binding)
        }

        private fun View.getPrefetchCount(item: Shelf.Lists<*>): Int {
            val itemWidth = when (item.type) {
                Shelf.Lists.Type.Linear -> when (item) {
                    is Shelf.Lists.Categories, is Shelf.Lists.Items -> 112
                    is Shelf.Lists.Tracks -> return 3
                }

                Shelf.Lists.Type.Grid -> return item.list.size
            }.dpToPx(context)
            val screenWidth = resources.displayMetrics.widthPixels
            val count = ceil(screenWidth.toFloat() / itemWidth)
            return count.roundToInt()
        }

        private fun Context.getLayoutManager(item: Shelf.Lists<*>): LinearLayoutManager {
            return if (item.type == Shelf.Lists.Type.Linear)
                LinearLayoutManager(this, HORIZONTAL, false)
            else GridLayoutManager(this, gridItemSpanCount())
        }

        fun Context.gridItemSpanCount(horizontalPadding: Int = 8 * 2): Int {
            val itemWidth = 176.dpToPx(this)
            val screenWidth = resources.displayMetrics.widthPixels
            val newWidth = screenWidth - horizontalPadding.dpToPx(this)
            val count = (newWidth.toFloat() / itemWidth).roundToInt()
            return count
        }
    }
}
