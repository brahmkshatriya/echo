package dev.brahmkshatriya.echo.ui.playlist.save

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemPlaylistSaveHeaderBinding

class TopAppBarAdapter(
    private val onClose: () -> Unit,
    private val onCreateClicked: () -> Unit
) : RecyclerView.Adapter<TopAppBarAdapter.ViewHolder>() {
    inner class ViewHolder(
        val binding: ItemPlaylistSaveHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setNavigationOnClickListener {
                onClose()
            }

            binding.root.setOnMenuItemClickListener {
                onCreateClicked()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlaylistSaveHeaderBinding.inflate(inflater, parent, false))

    }

    override fun getItemCount() = 1
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
}