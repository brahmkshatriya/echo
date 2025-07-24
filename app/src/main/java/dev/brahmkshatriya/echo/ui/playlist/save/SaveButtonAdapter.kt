package dev.brahmkshatriya.echo.ui.playlist.save

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemPlaylistSaveFooterBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter

class SaveButtonAdapter(
    private val onSave: () -> Unit
) : ScrollAnimRecyclerAdapter<SaveButtonAdapter.ViewHolder>(), GridAdapter {
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

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        notifyItemChanged(0)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.binding.root.isEnabled = enabled
    }
    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
}