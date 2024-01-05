package dev.brahmkshatriya.echo.ui.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemMainHeaderBinding

class HeaderAdapter(
    private val header: Int
) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {
    class HeaderViewHolder(val binding: ItemMainHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HeaderViewHolder(
        ItemMainHeaderBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.binding.topAppBarHeader.setText(header)
    }
}