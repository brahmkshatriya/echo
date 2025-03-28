package dev.brahmkshatriya.echo.ui.playlist.edit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemEditPlaylistHeaderBinding

class EditPlaylistHeaderAdapter(
    private val viewModel: EditPlaylistViewModel,
) : RecyclerView.Adapter<EditPlaylistHeaderAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemEditPlaylistHeaderBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEditPlaylistHeaderBinding.inflate(inflater, parent, false)
        binding.playlistName.doOnTextChanged { text, _, _, _ ->
            viewModel.playlistName.value = text.toString()
        }
        binding.playlistDescription.doOnTextChanged { text, _, _, _ ->
            viewModel.playlistDescription.value = text.toString()
        }
        return ViewHolder(binding)
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = viewModel.playlist
        holder.binding.playlistName.setText(playlist.title)
        holder.binding.playlistDescription.setText(playlist.description)
    }

}
