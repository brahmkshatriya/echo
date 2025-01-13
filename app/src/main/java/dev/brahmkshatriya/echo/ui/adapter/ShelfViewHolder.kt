package dev.brahmkshatriya.echo.ui.adapter

import android.app.Application
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfListsBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaListsBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.GridViewHolder.Companion.gridItemSpanCount
import dev.brahmkshatriya.echo.ui.adapter.GridViewHolder.Companion.ifGrid
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.applyIsPlaying
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.Media.Companion.bind
import dev.brahmkshatriya.echo.ui.adapter.ShowButtonViewHolder.Companion.ifShowingButton
import dev.brahmkshatriya.echo.ui.item.TrackAdapter
import dev.brahmkshatriya.echo.ui.item.TrackAdapter.Companion.applySwipe
import dev.brahmkshatriya.echo.utils.ui.dpToPx
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference
import javax.inject.Inject

sealed class ShelfViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    abstract fun bind(item: Shelf)
    abstract fun onCurrentChanged(current: Current?)

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
            val context = binding.root.context
            val (layoutManager, padding) = item.ifGrid {
                val count = binding.recyclerView.gridItemSpanCount()
                GridLayoutManager(context, count) to 16.dpToPx(context)
            } ?: item.ifShowingButton {
                LinearLayoutManager(context, RecyclerView.VERTICAL, false) to 0
            } ?: run {
                LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) to 16.dpToPx(context)
            }
            binding.recyclerView.updatePaddingRelative(start = padding, end = padding)
            layoutManager.apply {
                val state: Parcelable? = viewModel.layoutManagerStates[position]
                if (state != null) onRestoreInstanceState(state)
                else scrollToPosition(0)
            }
            viewModel.visibleScrollableViews[position] = WeakReference(this)

            val transition = transitionView.transitionName + item.id
            adapter.shelf = item
            adapter.transition = transition
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = layoutManager
        }

        val adapter = ShelfListItemViewAdapter(clientId, listener)

        override fun onCurrentChanged(current: Current?) {
            adapter.onCurrentChanged(binding.recyclerView, current)
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

        override fun onCurrentChanged(current: Current?) {}

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
            this.media = media
            isPlaying = binding.bind(media)
            binding.more.setOnClickListener {
                listener.onLongClick(clientId, media, transitionView)
            }
        }

        var media: EchoMediaItem? = null
        var isPlaying: MaterialButton? = null
        override fun onCurrentChanged(current: Current?) {
            applyIsPlaying(current, media?.id, isPlaying)
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

            fun ItemShelfMediaBinding.bind(item: EchoMediaItem): MaterialButton? {
                title.text = item.title
                subtitle.text = item.subtitleWithE
                subtitle.isVisible = item.subtitleWithE.isNullOrBlank().not()

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

    class MediaLists(
        val binding: ItemShelfMediaListsBinding,
        private val clientId: String,
        val listener: ShelfAdapter.Listener,
        val viewModel: ListViewModel,
        val observe: (PagedData.Single<Track>, suspend (PagingData<Track>) -> Unit) -> Job
    ) : ShelfViewHolder(binding.root) {
        override val transitionView: View
            get() = binding.root

        private var touchHelper: ItemTouchHelper? = null
        private var job: Job? = null
        override fun bind(item: Shelf) {
            val media = (item as? Shelf.Item)?.media ?: return
            if (media !is EchoMediaItem.Lists) return
            this.media = media
            isPlaying = binding.listsInfo.bind(media)

            binding.listsInfo.more.setOnClickListener {
                listener.onLongClick(clientId, media, transitionView)
            }
            binding.listsTracks.title.setText(R.string.songs)
            binding.listsTracks.shuffle.isVisible = false
            val transition = transitionView.transitionName + media.id
            val adapter = TrackAdapter(clientId, transition, listener, media, false)
            binding.listsTracks.recyclerView.adapter = adapter
            touchHelper?.attachToRecyclerView(null)
            touchHelper = binding.listsTracks.recyclerView.applySwipe(adapter)
            job?.cancel()
            val tracks = viewModel.loadTracks(clientId, media)
            job = observe(tracks) { adapter.submitData(it) }
        }

        var media: EchoMediaItem.Lists? = null
        var isPlaying: MaterialButton? = null
        override fun onCurrentChanged(current: Current?) {
            applyIsPlaying(current, media?.id, isPlaying)
        }

        companion object {
            fun create(
                parent: ViewGroup,
                clientId: String,
                listener: ShelfAdapter.Listener,
                viewModel: ListViewModel,
                observe: (PagedData.Single<Track>, suspend (PagingData<Track>) -> Unit) -> Job
            ): ShelfViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return MediaLists(
                    ItemShelfMediaListsBinding.inflate(layoutInflater, parent, false),
                    clientId,
                    listener,
                    viewModel,
                    observe
                )
            }
        }

        @HiltViewModel
        class ListViewModel @Inject constructor(
            val app: Application,
            val extensionList: MutableStateFlow<List<MusicExtension>?>,
        ) : ViewModel() {
            val map = hashMapOf<EchoMediaItem.Lists, PagedData.Single<Track>>()
            fun loadTracks(clientId: String, lists: EchoMediaItem.Lists) = map.getOrPut(lists) {
                PagedData.Single {
                    val client = extensionList.getExtension(clientId)?.instance?.value?.getOrNull()
                        ?: throw Exception(app.noClient().message)
                    when (lists) {
                        is EchoMediaItem.Lists.AlbumItem -> {
                            client as AlbumClient
                            val album = client.loadAlbum(lists.album)
                            client.loadTracks(album)
                        }

                        is EchoMediaItem.Lists.PlaylistItem -> {
                            client as PlaylistClient
                            val playlist = client.loadPlaylist(lists.playlist)
                            client.loadTracks(playlist)
                        }

                        is EchoMediaItem.Lists.RadioItem -> {
                            client as RadioClient
                            client.loadTracks(lists.radio)
                        }
                    }.loadFirst().take(3)
                }
            }
        }
    }
}