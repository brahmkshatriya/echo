package dev.brahmkshatriya.echo.ui.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.databinding.ItemAlbumInfoBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.utils.loadInto

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
        binding.albumArtistSubtitle.isVisible = false
        val art = album.artists.firstOrNull()
        (art as? Artist.WithCover)?.let {
            it.cover.loadInto(binding.albumArtistCover, R.drawable.art_artist)
            binding.albumArtistSubtitle.text = art.subtitle
            binding.albumArtistSubtitle.isVisible = !art.subtitle.isNullOrBlank()
        }
        binding.albumDescription.text = album.description
        binding.albumDescription.isVisible = !album.description.isNullOrBlank()
        var info = binding.root.context.getString(R.string.number_songs, album.tracks.size)
        album.duration?.toTimeString()?.let {
            info += " • $it"
        }
        album.releaseDate?.let {
            info += "\n$it"
        }
        album.publisher?.let {
            info += "\n$it"
        }
        println(info)
        binding.albumInfo.text = info
    }

    fun submit(album: Album.Full) {
        _album = album
    }
}