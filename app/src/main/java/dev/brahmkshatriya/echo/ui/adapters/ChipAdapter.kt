package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemChipBinding

class ChipAdapter(
    private val listener: (String) -> Unit
) : ListAdapter<Pair<Boolean, String>, ChipAdapter.ChipViewHolder>(ChipDiffCallback()) {
    class ChipDiffCallback : DiffUtil.ItemCallback<Pair<Boolean,String>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Boolean, String>,
            newItem: Pair<Boolean, String>
        ) = oldItem.second == newItem.second

        override fun areContentsTheSame(
            oldItem: Pair<Boolean, String>,
            newItem: Pair<Boolean, String>
        ) = oldItem == newItem

    }

    inner class ChipViewHolder(val binding: ItemChipBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.chip.setOnClickListener {
                listener.invoke(getItem(bindingAdapterPosition).second)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChipViewHolder(
        ItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            chip.text = item.second
            chip.isChecked = item.first
        }
    }
}