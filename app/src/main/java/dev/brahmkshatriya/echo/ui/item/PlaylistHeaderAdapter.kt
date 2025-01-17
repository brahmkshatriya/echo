package dev.brahmkshatriya.echo.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.ItemAlbumInfoBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemAlbumInfoBinding
import dev.brahmkshatriya.echo.utils.ui.toTimeString

class PlaylistHeaderAdapter(
    private val listener: Listener
) :
    RecyclerView.Adapter<PlaylistHeaderAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class Info(
            val binding: ItemAlbumInfoBinding,
            val playlist: Playlist,
            listener: Listener
        ) :
            ViewHolder(binding.root) {
            init {
                binding.albumPlay.setOnClickListener {
                    listener.onPlayClicked(playlist)
                }
                binding.albumRadio.setOnClickListener {
                    listener.onRadioClicked(playlist)
                }
                binding.btnSearch.setOnClickListener {
                    listener.onSearchClicked(playlist, it)
                }
                binding.btnSort.setOnClickListener {
                    listener.onSortClicked(playlist, it)
                }
            }
        }

        class ShimmerViewHolder(binding: SkeletonItemAlbumInfoBinding) :
            ViewHolder(binding.root)
    }

    interface Listener {
        fun onPlayClicked(list: Playlist)
        fun onRadioClicked(list: Playlist)
        fun onSortClicked(playlist: Playlist, view: View)
        fun onSearchClicked(playlist: Playlist, view: View)
    }

    override fun getItemViewType(position: Int) = if (_playlist == null) 0 else 1

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
            val playlist = _playlist!!
            ViewHolder.Info(
                ItemAlbumInfoBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                playlist,
                listener
            )
        }
    }

    private var _playlist: Playlist? = null
    private var _radio: Boolean = false

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder !is ViewHolder.Info) return
        val binding = holder.binding
        val playlist = holder.playlist
        binding.albumDescription.text = playlist.description
        binding.albumDescription.isVisible = !playlist.description.isNullOrBlank()
        var info = binding.root.context.run {
            playlist.tracks?.let {
                resources.getQuantityString(R.plurals.number_songs, it, it)
            } ?: getString(R.string.unknown_amount_songs)
        }
        playlist.duration?.toTimeString()?.let {
            info += " • $it"
        }
        playlist.creationDate?.let {
            info += "\n$it"
        }
        binding.albumInfo.text = info
        binding.albumRadio.isVisible = _radio
        binding.btnSearch.transitionName = "search_${playlist.id.hashCode()}"
        binding.btnSort.transitionName = "sort_${playlist.id.hashCode()}"
    }

    fun submit(playlist: Playlist, radio: Boolean) {
        _playlist = playlist
        _radio = radio
        notifyItemChanged(0)
    }
}