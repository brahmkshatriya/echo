package dev.brahmkshatriya.echo.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonGroup
import dev.brahmkshatriya.echo.databinding.ItemTabBinding
import dev.brahmkshatriya.echo.databinding.ItemTabContainerBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class TabsAdapter<T>(
    private val getTitle: T.() -> String,
    private val onTabSelected: (View, Int, T) -> Unit
) : ScrollAnimRecyclerAdapter<TabsAdapter.ViewHolder>(), GridAdapter {
    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)
    override fun getItemCount() = 1

    var data: List<T> = emptyList()
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    var selected = -1
        set(value) {
            field = value
            parent?.let { apply(it) }
        }

    var parent: MaterialButtonGroup? = null
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val parent = holder.binding.buttonGroup
        this.parent = parent
        apply(parent)
        parent.doOnLayout {
            if (selected < 0) return@doOnLayout
            val scrollX = parent.children.filter { it.isVisible }
                .take(selected).sumOf { it.width }
            holder.binding.root.scrollTo(scrollX, 0)
        }
    }

    fun apply(parent: MaterialButtonGroup) {
        val tabs = data
        parent.isVisible = tabs.isNotEmpty()
        if (tabs.isEmpty()) return
        val toKeep = tabs.size - parent.childCount
        val inflater = LayoutInflater.from(parent.context)
        if (toKeep > 0) repeat(toKeep) {
            parent.addView(ItemTabBinding.inflate(inflater, parent, false).root)
        } else if (toKeep < 0) repeat(-toKeep) {
            parent.getChildAt(tabs.size + it).isVisible = false
        }
        tabs.indices.forEach { i ->
            val tab = tabs[i]
            val button = parent.getChildAt(i) as MaterialButton
            button.apply {
                isVisible = true
                val title = getTitle(tab)
                if (text.toString() != title) text = title
                setOnClickListener(null)
                isChecked = i == selected
                setOnClickListener {
                    if (i == selected) isChecked = true
                    else onTabSelected(it, i, tab)
                }
            }
        }
    }

    class ViewHolder(
        parent: ViewGroup,
        val binding: ItemTabContainerBinding = ItemTabContainerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : ScrollAnimViewHolder(binding.root) {
        init {
            binding.buttonGroup.removeAllViews()
        }
    }
}