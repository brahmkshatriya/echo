package dev.brahmkshatriya.echo.ui.extensions.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.ItemExtensionAddBinding
import dev.brahmkshatriya.echo.databinding.ItemExtensionAddFooterBinding
import dev.brahmkshatriya.echo.databinding.ItemExtensionAddHeaderBinding
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle

class ExtensionsAddListAdapter(
    val listener: Listener
) : ListAdapter<ExtensionsAddListAdapter.Item, ExtensionsAddListAdapter.ViewHolder>(DiffCallback) {

    data class Item(
        val item: AddViewModel.ExtensionAssetResponse,
        val isChecked: Boolean,
        val isInstalled: Boolean
    )

    object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) =
            oldItem.item.id == newItem.item.id

        override fun areContentsTheSame(oldItem: Item, newItem: Item) =
            oldItem == newItem
    }

    fun interface Listener {
        fun onChecked(item: AddViewModel.ExtensionAssetResponse, isChecked: Boolean)
    }

    inner class ViewHolder(
        val binding: ItemExtensionAddBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.extensionSwitch.setOnCheckedChangeListener { _, checked ->
                val item = runCatching { getItem(bindingAdapterPosition) }.getOrNull()
                if (item == null) return@setOnCheckedChangeListener
                listener.onChecked(item.item, checked)
            }
            binding.root.setOnClickListener {
                binding.extensionSwitch.isChecked = !binding.extensionSwitch.isChecked
            }
            binding.extensionSwitch.isClickable = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExtensionAddBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, isChecked, isInstalled) = getItem(position)
        val binding = holder.binding
        binding.extensionName.text = if (!isInstalled) item.name
        else binding.root.context.getString(R.string.extension_installed, item.name)
        binding.extensionSubtitle.text = item.subtitle ?: item.id
        binding.itemExtension.apply {
            item.iconUrl?.toImageHolder().loadAsCircle(this, R.drawable.ic_extension_48dp) {
                setImageDrawable(it)
            }
        }
        binding.extensionSwitch.isChecked = isChecked
    }

    class Header(
        val listener: Listener
    ) : RecyclerView.Adapter<Header.ViewHolder>() {

        interface Listener {
            fun onClose()
            fun onSelectAllChanged(select: Boolean)
        }

        class ViewHolder(val binding: ItemExtensionAddHeaderBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemExtensionAddHeaderBinding.inflate(inflater, parent, false)
            binding.toolBar.setNavigationOnClickListener { listener.onClose() }
            binding.selectAll.setOnCheckedChangeListener { _, isChecked ->
                listener.onSelectAllChanged(isChecked)
            }
            return ViewHolder(binding)
        }

        override fun getItemCount() = 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    }

    class Footer(
        val listener: Listener
    ) : RecyclerView.Adapter<Footer.ViewHolder>() {

        fun interface Listener {
            fun onAdd()
        }

        class ViewHolder(val binding: ItemExtensionAddFooterBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemExtensionAddFooterBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount() = 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.root.setOnClickListener { listener.onAdd() }
        }

    }
}
