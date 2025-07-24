package dev.brahmkshatriya.echo.ui.playlist.edit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemEditPlaylistHeaderBinding
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter

class EditPlaylistHeaderAdapter(
    private val viewModel: EditPlaylistViewModel,
) : ScrollAnimRecyclerAdapter<EditPlaylistHeaderAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemEditPlaylistHeaderBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEditPlaylistHeaderBinding.inflate(inflater, parent, false)
        binding.playlistName.setOnEditorActionListener { v, actionId, event ->
            viewModel.nameFlow.value = v.text.toString()
            true
        }
        binding.playlistDescription.setOnEditorActionListener { v, actionId, event ->
            viewModel.descriptionFlow.value = v.text.toString()
            true
        }
        return ViewHolder(binding)
    }

    override fun getItemCount() = 1
    var pair: Pair<String, String?>? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val (title, desc) = pair ?: (null to null)
        holder.binding.playlistName.setText(title)
        holder.binding.playlistDescription.setText(desc)
    }

}
