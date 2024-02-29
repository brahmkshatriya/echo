package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.databinding.ItemAlbumInfoBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString

class AlbumHeaderAdapter(val listener: AlbumHeaderListener) :
    RecyclerView.Adapter<AlbumHeaderAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    inner class ViewHolder(val binding: ItemAlbumInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.albumPlay.setOnClickListener {
                _album?.let { it1 -> listener.onPlayClicked(it1) }
            }
            binding.albumShuffle.setOnClickListener {
                _album?.let { it1 -> listener.onShuffleClicked(it1) }
            }
        }
    }

    interface AlbumHeaderListener {
        fun onPlayClicked(album: Album.Full)
        fun onShuffleClicked(album: Album.Full)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAlbumInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    private var _album: Album.Full? = null
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = _album ?: return
        val binding = holder.binding
        binding.albumArtist.text = album.artists.joinToString(", ") { it.name }
        binding.albumDescription.text = album.description
        binding.albumDescription.isVisible = !album.description.isNullOrBlank()
        var subtitle = ""
        album.duration?.toTimeString()?.let {
            subtitle += it
        }
        album.releaseDate?.let {
            subtitle += " • $it"
        }
        binding.albumDuration.isVisible = subtitle.isNotEmpty()
    }

    fun submit(album: Album.Full) {
        _album = album
    }
}