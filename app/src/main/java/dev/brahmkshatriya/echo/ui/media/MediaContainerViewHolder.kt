package dev.brahmkshatriya.echo.ui.media

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.NewItemCategoryBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaBinding
import dev.brahmkshatriya.echo.ui.media.MediaItemViewHolder.Companion.bind
import java.lang.ref.WeakReference

sealed class MediaContainerViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(items: MediaItemsContainer)
    open val clickView = itemView
    abstract val transitionView: View


    class Category(
        val binding: NewItemCategoryBinding,
        val viewModel: MediaContainerAdapter.StateViewModel,
        private val clientId: String?,
        val listener: MediaItemAdapter.Listener,
    ) :
        MediaContainerViewHolder(binding.root) {
        override fun bind(items: MediaItemsContainer) {
            val category = items as MediaItemsContainer.Category
            binding.title.text = category.title
            binding.subtitle.text = category.subtitle
            binding.subtitle.isVisible = category.subtitle.isNullOrBlank().not()
            binding.recyclerView.adapter = MediaItemAdapter(listener, clientId, category.list)
            val position = bindingAdapterPosition
            binding.recyclerView.layoutManager?.apply {
                val state: Parcelable? = viewModel.layoutManagerStates[position]
                if (state != null) onRestoreInstanceState(state)
                else scrollToPosition(0)
            }
            viewModel.visibleScrollableViews[position] = WeakReference(this)
            binding.more.isVisible = category.more != null
        }

        val layoutManager get() = binding.recyclerView.layoutManager
        override val clickView: View = binding.more
        override val transitionView: View = binding.titleCard

        companion object {
            fun create(
                parent: ViewGroup,
                viewModel: MediaContainerAdapter.StateViewModel,
                clientId: String?,
                listener: MediaItemAdapter.Listener,
            ): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Category(
                    NewItemCategoryBinding.inflate(layoutInflater, parent, false),
                    viewModel,
                    clientId,
                    listener
                )
            }
        }
    }

    class Media(
        val binding: NewItemMediaBinding,
        private val clientId: String?,
        val listener: MediaItemAdapter.Listener,
    ) : MediaContainerViewHolder(binding.root) {
        override fun bind(items: MediaItemsContainer) {
            val item = (items as? MediaItemsContainer.Item)?.media ?: return
            binding.title.text = item.title
            binding.subtitle.text = item.subtitle
            binding.subtitle.isVisible = item.subtitle.isNullOrBlank().not()

            binding.trackImageContainer.root.isVisible = item is EchoMediaItem.TrackItem
            binding.listsImageContainer.root.isVisible = item is EchoMediaItem.Lists
            binding.profileImageContainer.root.isVisible = item is EchoMediaItem.Profile

            when (item) {
                is EchoMediaItem.TrackItem -> binding.trackImageContainer.bind(item)
                is EchoMediaItem.Lists -> binding.listsImageContainer.bind(item)
                is EchoMediaItem.Profile -> binding.profileImageContainer.bind(item)
            }

            binding.more.setOnClickListener { listener.onLongClick(clientId, item, transitionView) }
        }

        override val transitionView: View = binding.root

        companion object {
            fun create(
                parent: ViewGroup,
                clientId: String?,
                listener: MediaItemAdapter.Listener
            ): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Media(
                    NewItemMediaBinding.inflate(layoutInflater, parent, false),
                    clientId,
                    listener,
                )
            }
        }
    }
}