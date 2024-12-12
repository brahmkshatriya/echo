package dev.brahmkshatriya.echo.ui.item

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemExplicitBinding

class ExplicitAdapter(
    item: EchoMediaItem,
) : RecyclerView.Adapter<ExplicitAdapter.ViewHolder>() {
    private val isExplicit = item.isExplicit

    class ViewHolder(val binding: ItemExplicitBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExplicitBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = if (isExplicit) 1 else 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
}