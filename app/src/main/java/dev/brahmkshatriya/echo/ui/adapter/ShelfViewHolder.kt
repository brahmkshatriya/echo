package dev.brahmkshatriya.echo.ui.adapter

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent.SPACE_BETWEEN
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfListsBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.ui.adapter.GridViewHolder.Companion.ifGrid
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.adapter.ShowButtonViewHolder.Companion.ifShowingButton
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import java.lang.ref.WeakReference

sealed class ShelfViewHolder(
    itemView: View,
) : LifeCycleListAdapter.Holder<Shelf>(itemView) {

    open val clickView = itemView
    abstract val transitionView: View

    class Lists(
        val binding: ItemShelfListsBinding,
        val viewModel: ShelfAdapter.StateViewModel,
        private val sharedPool: RecyclerView.RecycledViewPool,
        private val clientId: String,
        val listener: ShelfAdapter.Listener,
    ) : ShelfViewHolder(binding.root) {

        override fun bind(item: Shelf) {
            item as Shelf.Lists<*>

            binding.title.text = item.title
            binding.subtitle.text = item.subtitle
            binding.subtitle.isVisible = item.subtitle.isNullOrBlank().not()
            binding.more.isVisible = item.more != null
            binding.shuffle.isVisible = if (item is Shelf.Lists.Tracks) {
                binding.shuffle.setOnClickListener {
                    listener.onShuffleClick(clientId, item)
                }
                true
            } else false
            binding.recyclerView.setRecycledViewPool(sharedPool)
            val position = bindingAdapterPosition
            val transition = transitionView.transitionName + item.id
            val context = binding.root.context
            val (layoutManager, padding) = item.ifGrid {
                FlexboxLayoutManager(context).apply {
                    justifyContent = SPACE_BETWEEN
                } to 16.dpToPx(context)
            } ?: item.ifShowingButton {
                LinearLayoutManager(context, RecyclerView.VERTICAL, false) to 0
            } ?: (LinearLayoutManager(
                context, RecyclerView.HORIZONTAL, false
            ) to 16.dpToPx(context))

            binding.recyclerView.updatePaddingRelative(start = padding, end = padding)
            layoutManager.apply {
                val state: Parcelable? = viewModel.layoutManagerStates[position]
                if (state != null) onRestoreInstanceState(state)
                else scrollToPosition(0)
            }
            viewModel.visibleScrollableViews[position] = WeakReference(this)
            val adapter = ShelfListItemViewAdapter(clientId, transition, listener, item)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = layoutManager
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
        override fun bind(item: Shelf) {
            item as Shelf.Category
            binding.title.text = item.title
            binding.subtitle.text = item.subtitle
            binding.subtitle.isVisible = item.subtitle.isNullOrBlank().not()
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
        val listener: ShelfAdapter.Listener,
    ) : ShelfViewHolder(binding.root) {
        override fun bind(item: Shelf) {
            val media = (item as? Shelf.Item)?.media ?: return
            val isPlaying = binding.bind(media)
            observe(listener.current) {
                val mediaItem = it?.mediaItem
                isPlaying(mediaItem?.mediaId == media.id || mediaItem?.context?.id == media.id)
            }
            binding.more.setOnClickListener {
                listener.onLongClick(clientId, media, transitionView)
            }
        }

        override val transitionView: View = binding.root

        companion object {
            fun create(
                parent: ViewGroup,
                clientId: String,
                listener: ShelfAdapter.Listener
            ): ShelfViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Media(
                    ItemShelfMediaBinding.inflate(layoutInflater, parent, false),
                    clientId,
                    listener,
                )
            }

            fun ItemShelfMediaBinding.bind(item: EchoMediaItem): (Boolean) -> Unit {
                title.text = item.title
                subtitle.text = item.subtitle
                subtitle.isVisible = item.subtitle.isNullOrBlank().not()

                trackImageContainer.root.isVisible = item is EchoMediaItem.TrackItem
                listsImageContainer.root.isVisible = item is EchoMediaItem.Lists
                profileImageContainer.root.isVisible = item is EchoMediaItem.Profile

                return when (item) {
                    is EchoMediaItem.TrackItem -> trackImageContainer.bind(item)
                    is EchoMediaItem.Lists -> listsImageContainer.bind(item)
                    is EchoMediaItem.Profile -> profileImageContainer.bind(item)
                }
            }
        }
    }
}