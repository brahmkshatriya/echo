package dev.brahmkshatriya.echo.ui.editplaylist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.ItemEditPlaylistHeaderBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.image.loadInto

class EditPlaylistHeader(
    val playlist: Playlist,
    val listener: Listener,
) : RecyclerView.Adapter<EditPlaylistHeader.ViewHolder>() {

    interface Listener {
        fun onCoverClicked()
        fun onUpdate(title: String, description: String?)
    }

    inner class ViewHolder(val binding: ItemEditPlaylistHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEditPlaylistHeaderBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        //TODO Cover Selection
        binding.coverContainer.isVisible = visible
        playlist.cover.loadInto(binding.cover, playlist.toMediaItem().placeHolder())
        binding.cover.setOnClickListener {
            listener.onCoverClicked()
        }

        fun updateMetadata() = listener.onUpdate(
            binding.playlistName.text.toString(),
            binding.playlistDescription.text.toString().ifEmpty { null }
        )

        binding.playlistName.apply {
            setText(playlist.title)
            setOnEditorActionListener { _, _, _ -> updateMetadata();false }
        }
        binding.playlistDescription.apply {
            setText(playlist.description)
            setOnEditorActionListener { _, _, _ -> updateMetadata();false }
        }
    }

    private var visible = false
    fun showCover(visible: Boolean) {
        this.visible = visible
        notifyItemChanged(0)
    }
}