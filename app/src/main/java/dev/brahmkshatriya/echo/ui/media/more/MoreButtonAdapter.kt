package dev.brahmkshatriya.echo.ui.media.more

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemMoreButtonBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter

class MoreButtonAdapter
    : ListAdapter<MoreButton, MoreButtonAdapter.ViewHolder>(MoreButton.DiffCallback), GridAdapter {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = 1

    class ViewHolder(
        parent: ViewGroup,
        val binding: ItemMoreButtonBinding = ItemMoreButtonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MoreButton) = with(binding.root) {
            text = item.title
            setOnClickListener { item.onClick() }
            setIconResource(item.icon)
        }
    }
}