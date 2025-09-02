package dev.brahmkshatriya.echo.ui.playlist.edit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ItemEditPlaylistHeaderBinding
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class EditPlaylistHeaderAdapter(
    private val fragment: EditPlaylistFragment,
    private val viewModel: EditPlaylistViewModel,
) : ScrollAnimRecyclerAdapter<EditPlaylistHeaderAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemEditPlaylistHeaderBinding) :
        ScrollAnimViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEditPlaylistHeaderBinding.inflate(inflater, parent, false)
        binding.playlistName.setOnEditorActionListener { v, actionId, event ->
            viewModel.nameFlow.value = v.text.toString()
            binding.playlistDescription.requestFocus()
            true
        }
        binding.playlistDescription.setOnEditorActionListener { v, actionId, event ->
            viewModel.descriptionFlow.value = v.text.toString()
            v.clearFocus()
            true
        }
        binding.coverContainer.setOnClickListener {
            viewModel.changeCover(fragment.requireActivity())
        }
        binding.removeCover.setOnClickListener {
            viewModel.coverFlow.value = EditPlaylistViewModel.CoverState.Removed
        }
        return ViewHolder(binding)
    }

    override fun getItemCount() = 1

    var data: EditPlaylistViewModel.Data? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val binding = holder.binding
        val (title, desc, coverEditable, cover) = data ?: return
        binding.playlistName.setText(title)
        binding.playlistDescription.setText(desc)
        binding.coverContainer.isVisible = coverEditable
        binding.removeCover.isVisible = cover != null
        cover.loadInto(binding.cover, R.drawable.art_add_photo)
    }

}