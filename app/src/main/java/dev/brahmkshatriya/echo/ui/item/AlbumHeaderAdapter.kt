package dev.brahmkshatriya.echo.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.databinding.ItemAlbumInfoBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemAlbumInfoBinding
import dev.brahmkshatriya.echo.utils.toTimeString

class AlbumHeaderAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<AlbumHeaderAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class Info(
            val binding: ItemAlbumInfoBinding,
            val album: Album,
            listener: Listener
        ) :
            ViewHolder(binding.root) {
            init {
                binding.albumPlay.setOnClickListener {
                    listener.onPlayClicked(album)
                }
                binding.albumRadio.setOnClickListener {
                    listener.onRadioClicked(album)
                }
            }
        }

        class ShimmerViewHolder(binding: SkeletonItemAlbumInfoBinding) :
            ViewHolder(binding.root)
    }

    interface Listener {
        fun onPlayClicked(album: Album)
        fun onRadioClicked(album: Album)
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

    private var _album: Album? = null
    private var _radio: Boolean = false

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder !is ViewHolder.Info) return
        val binding = holder.binding
        val album = holder.album
        binding.albumDescription.text = album.description
        binding.albumDescription.isVisible = !album.description.isNullOrBlank()
        var info = binding.root.context.run {
            album.tracks?.let {
                resources.getQuantityString(R.plurals.number_songs, it, it)
            } ?: getString(R.string.unknown_amount_songs)
        }
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
        binding.albumRadio.isVisible = _radio
    }

    fun submit(album: Album, radio: Boolean) {
        _album = album
        _radio = radio
        notifyItemChanged(0)
    }
}