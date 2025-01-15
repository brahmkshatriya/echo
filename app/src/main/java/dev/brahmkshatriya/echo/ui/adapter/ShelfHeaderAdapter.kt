package dev.brahmkshatriya.echo.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfHeaderBinding

class ShelfHeaderAdapter(
    private val clientId: String,
    private val title: String,
    private val listener: Listener
) : RecyclerView.Adapter<ShelfHeaderAdapter.ViewHolder>() {

    interface Listener {
        fun onShelfSearchClick(client: String, title: String, shelf: PagedData<Shelf>, view: View)
        fun onShelfSortClick(client: String, title: String, shelf: PagedData<Shelf>, view: View)
    }

    class ViewHolder(val binding: ItemShelfHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemShelfHeaderBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.isVisible = _shelf != null
        val shelf = _shelf ?: return
        holder.binding.run {
            btnSearch.transitionName = "search_${shelf.hashCode()}"
            btnSort.transitionName = shelf.hashCode().toString()
            btnSearch.setOnClickListener { listener.onShelfSearchClick(clientId, title, shelf, it) }
            btnSort.setOnClickListener { listener.onShelfSortClick(clientId, title, shelf, it) }
        }
    }

    private var _shelf: PagedData<Shelf>? = null
    fun setShelf(shelf: PagedData<Shelf>?) {
        _shelf = shelf
        notifyItemChanged(0)
    }
}