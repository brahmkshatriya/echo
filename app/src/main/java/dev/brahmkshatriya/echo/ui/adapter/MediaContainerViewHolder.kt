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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.NewItemCategoryBinding
import dev.brahmkshatriya.echo.databinding.NewItemContainerBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaBinding
import dev.brahmkshatriya.echo.databinding.NewItemTracksBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.item.TrackAdapter
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

sealed class MediaContainerViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView), LifecycleOwner {
    abstract fun bind(container: MediaItemsContainer)
    open val clickView = itemView
    abstract val transitionView: View

    lateinit var lifecycleRegistry : LifecycleRegistry
    val isInitialized get() = this::lifecycleRegistry.isInitialized
    override val lifecycle get() = lifecycleRegistry

    class Category(
        val binding: NewItemCategoryBinding,
        val viewModel: MediaContainerAdapter.StateViewModel,
        private val sharedPool: RecyclerView.RecycledViewPool,
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
            binding.recyclerView.setRecycledViewPool(sharedPool)
            val position = bindingAdapterPosition
            val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
            layoutManager.apply {
                initialPrefetchItemCount = category.list.size.coerceAtMost(4)
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
                sharedPool: RecyclerView.RecycledViewPool,
                clientId: String?,
                listener: MediaItemAdapter.Listener,
            ): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Category(
                    NewItemCategoryBinding.inflate(layoutInflater, parent, false),
                    viewModel,
                    sharedPool,
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

            fun NewItemMediaBinding.bind(item: EchoMediaItem) {
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

    class MediaTracks(
        val binding: NewItemTracksBinding,
        private val sharedPool: RecyclerView.RecycledViewPool,
        private val clientId: String?,
        val listener: MediaItemAdapter.Listener
    ) : MediaContainerViewHolder(binding.root) {

        private val trackListener = object : TrackAdapter.Listener {
            override fun onClick(list: List<Track>, position: Int, view: View) {
                listener.onClick(clientId, list[position].toMediaItem(), view)
            }

            override fun onLongClick(list: List<Track>, position: Int, view: View): Boolean {
                return listener.onLongClick(clientId, list[position].toMediaItem(), view)
            }
        }

        override fun bind(container: MediaItemsContainer) {
            val tracks = container as MediaItemsContainer.Tracks
            binding.title.text = tracks.title
            binding.subtitle.text = tracks.subtitle
            binding.subtitle.isVisible = tracks.subtitle.isNullOrBlank().not()
            val adapter = TrackAdapter(
                transitionView.transitionName + tracks.id,
                trackListener
            )
            binding.recyclerView.adapter = adapter
            binding.recyclerView.setRecycledViewPool(sharedPool)
            binding.more.isVisible = tracks.more != null
            lifecycleScope.launch { adapter.submit(PagingData.from(tracks.list.take(6))) }
        }

        val layoutManager get() = binding.recyclerView.layoutManager
        override val clickView: View = binding.more
        override val transitionView: View = binding.titleCard

        companion object {
            fun create(
                parent: ViewGroup,
                sharedPool: RecyclerView.RecycledViewPool,
                clientId: String?,
                listener: MediaItemAdapter.Listener,
            ): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return MediaTracks(
                    NewItemTracksBinding.inflate(layoutInflater, parent, false),
                    sharedPool,
                    clientId,
                    listener
                )
            }
        }
    }
}