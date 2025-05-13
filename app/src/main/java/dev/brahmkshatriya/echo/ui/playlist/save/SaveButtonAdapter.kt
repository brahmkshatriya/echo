package dev.brahmkshatriya.echo.ui.playlist.save

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemPlaylistSaveFooterBinding

class SaveButtonAdapter(
    private val onSave: () -> Unit
) : RecyclerView.Adapter<SaveButtonAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemPlaylistSaveFooterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlaylistSaveFooterBinding.inflate(inflater, parent, false)
        binding.root.setOnClickListener { onSave() }
        return ViewHolder(binding)
    }

    override fun getItemCount() = 1
    private var enabled = false

    @SuppressLint("NotifyDataSetChanged")
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.isEnabled = enabled
    }
}