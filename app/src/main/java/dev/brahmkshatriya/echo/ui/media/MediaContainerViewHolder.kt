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
import dev.brahmkshatriya.echo.databinding.NewItemContainerBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaBinding
import dev.brahmkshatriya.echo.ui.media.MediaItemViewHolder.Companion.bind
import java.lang.ref.WeakReference

sealed class MediaContainerViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(container: MediaItemsContainer)
    open val clickView = itemView
    abstract val transitionView: View

    class Category(
        val binding: NewItemCategoryBinding,
        val viewModel: MediaContainerAdapter.StateViewModel,
        private val clientId: String?,
        val listener: MediaItemAdapter.Listener,
    ) :
        MediaContainerViewHolder(binding.root) {
        override fun bind(container: MediaItemsContainer) {
            val category = container as MediaItemsContainer.Category
            binding.title.text = category.title
            binding.subtitle.text = category.subtitle
            binding.subtitle.isVisible = category.subtitle.isNullOrBlank().not()
            binding.recyclerView.adapter =
                MediaItemAdapter(
                    listener,
                    transitionView.transitionName + category.id,
                    clientId,
                    category.list
                )
            binding.recyclerView.setHasFixedSize(true)
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

    class Container(
        val binding: NewItemContainerBinding
    ) : MediaContainerViewHolder(binding.root) {
        override fun bind(container: MediaItemsContainer) {
            container as MediaItemsContainer.Container
            binding.title.text = container.title
            binding.subtitle.text = container.subtitle
            binding.subtitle.isVisible = container.subtitle.isNullOrBlank().not()
        }

        override val transitionView = binding.root

        companion object {
            fun create(parent: ViewGroup): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Container(
                    NewItemContainerBinding.inflate(layoutInflater, parent, false)
                )
            }
        }

    }

    class Media(
        val binding: NewItemMediaBinding,
        private val clientId: String?,
        val listener: MediaItemAdapter.Listener,
    ) : MediaContainerViewHolder(binding.root) {
        override fun bind(container: MediaItemsContainer) {
            val item = (container as? MediaItemsContainer.Item)?.media ?: return
            binding.bind(item)
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
            
            fun NewItemMediaBinding.bind(item:EchoMediaItem){
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