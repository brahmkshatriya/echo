package dev.brahmkshatriya.echo.ui.adapter

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfListsBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.item.TrackAdapter
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

sealed class ShelfViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView), LifecycleOwner {
    abstract fun bind(container: Shelf)
    open val clickView = itemView
    abstract val transitionView: View

    lateinit var lifecycleRegistry: LifecycleRegistry
    val isInitialized get() = this::lifecycleRegistry.isInitialized
    override val lifecycle get() = lifecycleRegistry

    class Lists(
        val binding: ItemShelfListsBinding,
        val viewModel: ShelfAdapter.StateViewModel,
        private val sharedPool: RecyclerView.RecycledViewPool,
        private val clientId: String,
        val listener: ShelfAdapter.Listener,
    ) :
        ShelfViewHolder(binding.root) {
        override fun bind(container: Shelf) {
            val lists = container as Shelf.Lists<*>
            binding.title.text = lists.title
            binding.subtitle.text = lists.subtitle
            binding.subtitle.isVisible = lists.subtitle.isNullOrBlank().not()
            binding.more.isVisible = lists.more != null

            binding.recyclerView.setRecycledViewPool(sharedPool)
            val position = bindingAdapterPosition
            val transition = transitionView.transitionName + lists.id
            binding.recyclerView.adapter = when (lists) {
                is Shelf.Lists.Categories ->
                    CategoryAdapter(clientId, transition, listener, lists.list)

                is Shelf.Lists.Items ->
                    MediaItemAdapter(clientId, transition, listener, lists.list)

                is Shelf.Lists.Tracks -> {
                    val adapter = TrackAdapter(clientId, transition, listener)
                    lifecycleScope.launch { adapter.submit(PagingData.from(lists.list)) }
                    adapter
                }
            }


            val layoutManager = binding.recyclerView.layoutManager as RecyclerView.LayoutManager
            layoutManager.apply {
                val state: Parcelable? = viewModel.layoutManagerStates[position]
                if (state != null) onRestoreInstanceState(state)
                else scrollToPosition(0)
            }
            viewModel.visibleScrollableViews[position] = WeakReference(this)

        }

        val layoutManager get() = binding.recyclerView.layoutManager
        override val clickView: View = binding.more
        override val transitionView: View = binding.titleCard

        companion object {
            fun create(
                parent: ViewGroup,
                viewModel: ShelfAdapter.StateViewModel,
                sharedPool: RecyclerView.RecycledViewPool,
                clientId: String,
                listener: ShelfAdapter.Listener,
            ): ShelfViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Lists(
                    ItemShelfListsBinding.inflate(layoutInflater, parent, false),
                    viewModel,
                    sharedPool,
                    clientId,
                    listener
                )
            }
        }
    }

    class Category(
        val binding: ItemShelfCategoryBinding
    ) : ShelfViewHolder(binding.root) {
        override fun bind(container: Shelf) {
            container as Shelf.Category
            binding.title.text = container.title
            binding.subtitle.text = container.subtitle
            binding.subtitle.isVisible = container.subtitle.isNullOrBlank().not()
        }

        override val transitionView = binding.root

        companion object {
            fun create(parent: ViewGroup): ShelfViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Category(
                    ItemShelfCategoryBinding.inflate(layoutInflater, parent, false)
                )
            }
        }

    }

    class Media(
        val binding: ItemShelfMediaBinding,
        private val clientId: String,
        val listener: MediaItemAdapter.Listener,
    ) : ShelfViewHolder(binding.root) {
        override fun bind(container: Shelf) {
            val item = (container as? Shelf.Item)?.media ?: return
            binding.bind(item)
            binding.more.setOnClickListener { listener.onLongClick(clientId, item, transitionView) }
        }

        override val transitionView: View = binding.root

        companion object {
            fun create(
                parent: ViewGroup,
                clientId: String,
                listener: MediaItemAdapter.Listener
            ): ShelfViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Media(
                    ItemShelfMediaBinding.inflate(layoutInflater, parent, false),
                    clientId,
                    listener,
                )
            }

            fun ItemShelfMediaBinding.bind(item: EchoMediaItem) {
                title.text = item.title
                subtitle.text = item.subtitle
                subtitle.isVisible = item.subtitle.isNullOrBlank().not()

                trackImageContainer.root.isVisible = item is EchoMediaItem.TrackItem
                listsImageContainer.root.isVisible = item is EchoMediaItem.Lists
                profileImageContainer.root.isVisible = item is EchoMediaItem.Profile

                when (item) {
                    is EchoMediaItem.TrackItem -> trackImageContainer.bind(item)
                    is EchoMediaItem.Lists -> listsImageContainer.bind(item)
                    is EchoMediaItem.Profile -> profileImageContainer.bind(item)
                }
            }
        }
    }
}