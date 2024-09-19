package dev.brahmkshatriya.echo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding

class CategoryAdapter(
    private val clientId: String,
    private val transition: String,
    private val listener: ShelfAdapter.Listener,
    private val list: List<Shelf.Category>
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemShelfCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemShelfCategoryBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val binding = holder.binding
        val root = binding.root
        root.transitionName = (transition + item.id).hashCode().toString()
        root.updateLayoutParams { width = ViewGroup.LayoutParams.WRAP_CONTENT }
        binding.title.text = item.title
        binding.subtitle.text = item.subtitle
        binding.subtitle.isVisible = item.subtitle.isNullOrBlank().not()

        holder.itemView.setOnClickListener {
            listener.onClick(clientId, item, root)
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(clientId, item, root)
        }
    }
}