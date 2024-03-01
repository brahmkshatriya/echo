package dev.brahmkshatriya.echo.ui.album

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.google.android.material.transition.MaterialElevationScale
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentCollapsingBarBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackSmallBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class AlbumFragment : Fragment() {

    private var binding: FragmentCollapsingBarBinding by autoCleared()
    private val viewModel: AlbumViewModel by viewModels()
    private val extensionViewModel: ExtensionViewModel by activityViewModels()
    private val args: AlbumFragmentArgs by navArgs()

    private val clickListener = MediaItemClickListener(this)
    private val trackAdapter = TrackAdapter(clickListener, false)
    private val mediaItemsContainerAdapter = MediaItemsContainerAdapter(this, clickListener)
    private val header = AlbumHeaderAdapter(object : AlbumHeaderAdapter.AlbumHeaderListener {
        override fun onPlayClicked(album: Album.Full) {
            Toast.makeText(context, "Todo", Toast.LENGTH_SHORT).show()
        }

        override fun onShuffleClicked(album: Album.Full) {
            Toast.makeText(context, "Todo", Toast.LENGTH_SHORT).show()
        }
    })
    private val concatAdapter = ConcatAdapter(header, trackAdapter, mediaItemsContainerAdapter)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCollapsingBarBinding.inflate(inflater, container, false)
        enterTransition = MaterialElevationScale(true)
        exitTransition = MaterialElevationScale(true)
        reenterTransition = MaterialElevationScale(true)
        returnTransition = MaterialElevationScale(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.albumCoverContainer.updatePadding(top = insets.top)
            windowInsets
        }
        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            val inverted = 1 - offset
            binding.endIcon.alpha = inverted
            binding.toolbarOutline.alpha = offset
        }
        binding.toolbar.setupWithNavController(findNavController())
        postponeEnterTransition()
        binding.recyclerView.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        val album: Album.Small = args.albumWithCover ?: args.albumSmall ?: return

        binding.root.transitionName = album.uri.toString()
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }
        binding.toolbar.title = album.title.trim()

        (album as? Album.WithCover).let {
            it?.numberOfTracks?.let { it1 ->
                albumImage(it1, binding.albumCover1, binding.albumCover2)
                it.cover.loadInto(binding.albumCover1, R.drawable.art_album)
                it.cover.loadInto(binding.albumCover2, R.drawable.art_album)
            }
            it?.cover.loadInto(binding.albumCover, R.drawable.art_album)
        }

        observe(extensionViewModel.extensionFlow) {
            binding.recyclerView.adapter = getAdapterForExtension<AlbumClient>(
                it, R.string.album, concatAdapter, true
            ) { client ->
                if (client == null) return@getAdapterForExtension
                viewModel.loadAlbum(client, album)
            }
        }
        observe(viewModel.albumFlow) {
            if (it != null) {
                trackAdapter.submitList(it.tracks)
                header.submit(it)
            }
        }
        observe(viewModel.result) {
            if (it != null) mediaItemsContainerAdapter.submit(it)
        }
    }
}

class TrackAdapter(
    private val callback: MediaItemClickListener,
    private val albumVisible: Boolean = true,
) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    var list: List<Track>? = null

    inner class ViewHolder(val binding: ItemTrackSmallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val track = list?.get(bindingAdapterPosition) ?: return@setOnClickListener
                callback.onClick(binding.imageView to track.toMediaItem())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTrackSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = list?.count() ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val track = list?.get(position) ?: return
        binding.itemNumber.text =
            binding.root.context.getString(R.string.number_dot, (position + 1))
        binding.itemTitle.text = track.title
        track.cover.loadInto(binding.imageView, R.drawable.art_music)
        var subtitle = ""
        track.duration?.toTimeString()?.let {
            subtitle += it
        }
        track.artists.joinToString(", ") { it.name }.let {
            if (it.isNotBlank()) subtitle += if (subtitle.isNotBlank()) " • $it" else it
        }
        binding.itemSubtitle.isVisible = subtitle.isNotEmpty()
        binding.itemSubtitle.text = subtitle
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(tracks: List<Track>) {
        list = tracks
        notifyDataSetChanged()
    }
}
