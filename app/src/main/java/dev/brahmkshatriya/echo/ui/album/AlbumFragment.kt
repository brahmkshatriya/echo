package dev.brahmkshatriya.echo.ui.album

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentCollapsingBarBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackBinding
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val album: Album.Small = args.albumWithCover ?: args.albumSmall ?: return

        val adapter = TrackAdapter(MediaItemClickListener(this), true)

        binding.toolbar.title = album.title
        (album as? Album.WithCover)?.let {
            it.cover.loadInto(binding.albumCover)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){ _, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            println(systemInsets)
            binding.albumCoverContainer.updatePadding(top = systemInsets.top)
            insets
        }
        binding.albumCoverContainer.requestApplyInsets()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = getAdapterForExtension<AlbumClient>(
            extensionViewModel.getCurrentExtension(), R.string.album, adapter, true
        ) { client ->
            if (client == null) return@getAdapterForExtension
            viewModel.loadAlbum(client, album)
            observe(viewModel.albumFlow) {
                it ?: return@observe
                adapter.submitList(it.tracks)
            }
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
                callback.onClick(track.toMediaItem())
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
