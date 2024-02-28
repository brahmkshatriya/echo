package dev.brahmkshatriya.echo.ui.album

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialContainerTransform
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentCollapsingBarBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackBinding
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe

class AlbumFragment : Fragment() {

    private var binding: FragmentCollapsingBarBinding by autoCleared()
    private val extensionViewModel: ExtensionViewModel by activityViewModels()
    private val viewModel: AlbumViewModel by viewModels()

    private val args: AlbumFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCollapsingBarBinding.inflate(inflater, container, false)

        val album: Album.Small = args.albumWithCover ?: args.albumSmall ?: return binding.root
        binding.albumCover.transitionName = album.uri.toString()
        sharedElementEnterTransition = MaterialContainerTransform()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val album: Album.Small = args.albumWithCover ?: args.albumSmall ?: return

        val clickListener = MediaItemClickListener(this)
        val trackAdapter = TrackAdapter(clickListener, false)
        val mediaItemsContainerAdapter = MediaItemsContainerAdapter(lifecycle, clickListener)
        val concatAdapter = ConcatAdapter(trackAdapter, mediaItemsContainerAdapter)

        binding.toolbar.title = album.title.trim()
        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            val inverted = 1 - offset
            binding.endIcon.alpha = inverted
            binding.albumCover.alpha = inverted
            binding.toolbarOutline.alpha = offset
        }

        (album as? Album.WithCover).let {
            it?.cover.loadInto(binding.albumCover, R.drawable.art_album)
        }

        binding.toolbar.setupWithNavController(findNavController())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = getAdapterForExtension<AlbumClient>(
            extensionViewModel.getCurrentExtension(), R.string.album, concatAdapter, true
        ) { client ->
            if (client == null) return@getAdapterForExtension
            viewModel.loadAlbum(client, album)
        }

        observe(viewModel.albumFlow) {
            if (it != null) trackAdapter.submitList(it.tracks)
        }

        observe(viewModel.result) {
            if (it != null) mediaItemsContainerAdapter.submitData(it)
        }
    }
}

class TrackAdapter(
    private val callback: MediaItemClickListener,
    private val albumVisible: Boolean = true,
) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    var list: List<Track>? = null

    inner class ViewHolder(val binding: ItemTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val track = list?.get(bindingAdapterPosition) ?: return@setOnClickListener
                callback.onClick(binding.imageView to track.toMediaItem())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = list?.count() ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val track = list?.get(position) ?: return
        binding.title.text = track.title
        track.cover.loadInto(binding.imageView, R.drawable.art_music)
        binding.artist.text = track.artists.joinToString(", ") { it.name }
        binding.album.isVisible = albumVisible
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(tracks: List<Track>) {
        list = tracks
        notifyDataSetChanged()
    }
}
