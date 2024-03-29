package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.ItemAlbumBinding
import dev.brahmkshatriya.echo.databinding.ItemArtistBinding
import dev.brahmkshatriya.echo.databinding.ItemCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemPlaylistBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackBinding
import dev.brahmkshatriya.echo.newui.media.MediaContainerLoadingAdapter
import dev.brahmkshatriya.echo.newui.exception.openException
import dev.brahmkshatriya.echo.ui.ClickListener
import dev.brahmkshatriya.echo.ui.MediaItemClickListener

class MediaItemsContainerAdapter(
    val fragment: Fragment,
    private val listener: ClickListener<Pair<View, MediaItemsContainer>> = MediaItemClickListener(
        fragment
    ),
) : PagingDataAdapter<MediaItemsContainer, MediaItemsContainerAdapter.MediaItemsContainerHolder>(
    MediaItemsContainerComparator
) {

    private val lifecycle = fragment.lifecycle

    fun withLoadingFooter(): ConcatAdapter {
        val footer = MediaContainerLoadingAdapter(object : MediaContainerLoadingAdapter.Listener {
            override fun onRetry() {
                retry()
            }

            override fun onError(error: Throwable) {
                fragment.requireActivity().openException(
                    fragment.findNavController(),
                    error
                )
            }
        })
        addLoadStateListener { loadStates ->
            footer.loadState = when (loadStates.refresh) {
                is LoadState.NotLoading -> loadStates.append
                else -> loadStates.refresh
            }
        }
        return ConcatAdapter(this, footer)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.let {
            when (it) {
                is MediaItemsContainer.Category -> 0
                else -> null
            }
        } ?: -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> Category(
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        1 -> Track(
            ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        2 -> Album(
            ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        3 -> Artist(
            ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        4 -> Playlist(
            ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        else -> throw IllegalArgumentException("Unknown view type: $viewType")
    }


    override fun onBindViewHolder(holder: MediaItemsContainerHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind()

        holder.clickView.apply {
            setOnClickListener {
                listener.onClick(this to item)
            }
            setOnLongClickListener {
                listener.onLongClick(this to item)
                true
            }
        }
    }
    suspend fun submit(pagingData: PagingData<MediaItemsContainer>) {
//        saveState()
        submitData(pagingData)
    }
    //NESTED RECYCLER VIEW THINGS

//    private val stateViewModel: StateViewModel by fragment.viewModels()
//
//    class StateViewModel : ViewModel() {
//        val layoutManagerStates = hashMapOf<Int, Parcelable?>()
//        val visibleScrollableViews = hashMapOf<Int, WeakReference<Category>>()
//    }
//



    // VIEW HOLDER

    abstract inner class MediaItemsContainerHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        abstract fun bind()
        abstract val clickView: View
    }

    inner class Category(val binding: ItemCategoryBinding) :
        MediaItemsContainerHolder(binding.root) {
        private val adapter = MediaItemAdapter(listener)
        val layoutManager = LinearLayoutManager(binding.root.context, HORIZONTAL, false)
        override fun bind() {
            val item = getItem(bindingAdapterPosition) ?: return
            val category = item as MediaItemsContainer.Category
            binding.textView.text = category.title
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.adapter = adapter
            adapter.submitData(lifecycle, category.list)
//            layoutManager.let {
//                val state: Parcelable? = stateViewModel.layoutManagerStates[position]
//                if (state != null) it.onRestoreInstanceState(state)
//                else it.scrollToPosition(0)
//            }
//            stateViewModel.visibleScrollableViews[position] = WeakReference(this)
            binding.more.isVisible = category.more != null
            binding.cardView.transitionName = category.more.hashCode().toString()
        }

        override val clickView = binding.cardView
    }

    inner class Track(val binding: ItemTrackBinding) : MediaItemsContainerHolder(binding.root) {
        override fun bind() {
//            val item = getItem(bindingAdapterPosition) ?: return
//            val track = (item as MediaItemsContainer.TrackItem).track
//            binding.title.text = track.title
//            track.cover.loadInto(binding.imageView, R.drawable.art_music)
//
//            binding.album.isVisible = track.album != null
//            binding.album.text = track.album?.title
//
//            binding.artist.isVisible = track.artists.isNotEmpty()
//            binding.artist.text = track.artists.joinToString(" ") { it.name }
//
//            binding.duration.isVisible = track.duration != null
//            binding.duration.text = track.duration?.toTimeString()
//
//            binding.root.transitionName = track.id
        }

        override val clickView = binding.root
    }

    inner class Album(val binding: ItemAlbumBinding) : MediaItemsContainerHolder(binding.root) {
        override fun bind() {
//            val item = getItem(bindingAdapterPosition) ?: return
//            val album = (item as MediaItemsContainer.AlbumItem).album
//            binding.title.text = album.title
//            album.cover.apply {
//                loadWith(binding.imageView, R.drawable.art_album, null) {
//                    loadInto(binding.imageView1, R.drawable.art_album)
//                    loadInto(binding.imageView2, R.drawable.art_album)
//                }
//            }
//            albumImage(album.numberOfTracks, binding.imageView1, binding.imageView2)
//            binding.artist.isVisible = album.artists.isNotEmpty()
//            binding.artist.text = album.artists.joinToString(" ") { it.name }
//
//            binding.duration.isVisible = album.numberOfTracks != null
//            binding.duration.text = album.numberOfTracks?.let {
//                binding.root.context.resources.getQuantityString(
//                    R.plurals.number_songs, it, album.numberOfTracks
//                )
//            }
//
//            binding.subtitle.isVisible = !album.subtitle.isNullOrBlank()
//            binding.subtitle.text = album.subtitle
//
//            binding.root.transitionName = album.id
        }

        override val clickView = binding.root
    }

    inner class Artist(val binding: ItemArtistBinding) : MediaItemsContainerHolder(binding.root) {
        override fun bind() {
//            val item = getItem(bindingAdapterPosition) ?: return
//            val artist = (item as MediaItemsContainer.ArtistItem).artist
//            binding.title.text = artist.name
//
//            binding.subtitle.isVisible = !artist.subtitle.isNullOrBlank()
//            binding.subtitle.text = artist.subtitle
//
//            artist.cover.loadInto(binding.imageView, R.drawable.art_artist)
//            binding.root.transitionName = artist.id
        }

        override val clickView = binding.root
    }

    inner class Playlist(val binding: ItemPlaylistBinding) :
        MediaItemsContainerHolder(binding.root) {
        override fun bind() {
//            val item = getItem(bindingAdapterPosition) ?: return
//            val playlist = (item as MediaItemsContainer.PlaylistItem).playlist
//            binding.title.text = playlist.title
//            playlist.cover.apply {
//                loadWith(binding.imageView, R.drawable.art_library_music, null) {
//                    loadInto(binding.imageView1, R.drawable.art_library_music)
//                    loadInto(binding.imageView2, R.drawable.art_library_music)
//                }
//            }
//            albumImage(3, binding.imageView1, binding.imageView2)
//            binding.subtitle.text = playlist.subtitle
//            binding.subtitle.isVisible = !playlist.subtitle.isNullOrBlank()
//            binding.root.transitionName = playlist.id
        }

        override val clickView = binding.root
    }

    companion object MediaItemsContainerComparator : DiffUtil.ItemCallback<MediaItemsContainer>() {

        override fun areItemsTheSame(
            oldItem: MediaItemsContainer, newItem: MediaItemsContainer
        ): Boolean {
            return false
        }

        override fun areContentsTheSame(
            oldItem: MediaItemsContainer, newItem: MediaItemsContainer
        ): Boolean {
            return true
        }
    }
}