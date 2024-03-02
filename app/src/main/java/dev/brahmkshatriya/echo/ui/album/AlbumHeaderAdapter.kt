package dev.brahmkshatriya.echo.ui.album

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.databinding.ItemAlbumInfoBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemAlbumInfoBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.utils.loadInto

class AlbumHeaderAdapter(private val listener: AlbumHeaderListener) :
    RecyclerView.Adapter<AlbumHeaderAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class Info(
            val binding: ItemAlbumInfoBinding,
            val album: Album.Full,
            listener: AlbumHeaderListener
        ) :
            ViewHolder(binding.root) {
            init {
                binding.albumPlay.setOnClickListener {
                    album.let { it1 -> listener.onPlayClicked(it1) }
                }
                binding.albumShuffle.setOnClickListener {
                    album.let { it1 -> listener.onShuffleClicked(it1) }
                }
            }
        }

        class ShimmerViewHolder(binding: SkeletonItemAlbumInfoBinding) :
            ViewHolder(binding.root)
    }

    interface AlbumHeaderListener {
        fun onPlayClicked(album: Album.Full)
        fun onShuffleClicked(album: Album.Full)
    }

    override fun getItemViewType(position: Int) = if (_album == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            ViewHolder.ShimmerViewHolder(
                SkeletonItemAlbumInfoBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        } else {
            val album = _album!!
            ViewHolder.Info(
                ItemAlbumInfoBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                album,
                listener
            )
        }
    }

    private var _album: Album.Full? = null
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder !is ViewHolder.Info) return
        val binding = holder.binding
        val album = holder.album
        binding.albumArtist.text = album.artist.name
        binding.albumArtistSubtitle.isVisible = false
        val art = album.artist
        (art as? Artist.WithCover)?.let {
            it.cover.loadInto(binding.albumArtistCover, R.drawable.art_artist)
            binding.albumArtistSubtitle.text = art.subtitle
            binding.albumArtistSubtitle.isVisible = !art.subtitle.isNullOrBlank()
        }
        binding.albumDescription.text = album.description
        binding.albumDescription.isVisible = !album.description.isNullOrBlank()
        var info = binding.root.context.resources.getQuantityString(
            R.plurals.number_songs,
            album.tracks.size,
            album.tracks.size
        )
        album.duration?.toTimeString()?.let {
            info += " • $it"
        }
        album.releaseDate?.let {
            info += "\n$it"
        }
        album.publisher?.let {
            info += "\n$it"
        }
        binding.albumInfo.text = info
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submit(album: Album.Full) {
        println("submitted")
        _album = album
        notifyItemChanged(0)
    }
}