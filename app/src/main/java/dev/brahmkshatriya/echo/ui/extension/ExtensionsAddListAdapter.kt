package dev.brahmkshatriya.echo.ui.extension

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.ItemExtensionAddBinding
import dev.brahmkshatriya.echo.databinding.ItemExtensionAddFooterBinding
import dev.brahmkshatriya.echo.databinding.ItemExtensionAddHeaderBinding
import dev.brahmkshatriya.echo.extensions.ExtensionAssetResponse
import dev.brahmkshatriya.echo.utils.loadAsCircle

class ExtensionsAddListAdapter(
    val map: List<Item>,
    val listener: Listener
) : RecyclerView.Adapter<ExtensionsAddListAdapter.ViewHolder>() {

    data class Item(
        val item: ExtensionAssetResponse,
        val isChecked: Boolean,
        val isInstalled: Boolean
    )

    fun interface Listener {
        fun onChecked(item: ExtensionAssetResponse, isChecked: Boolean)
    }

    inner class ViewHolder(val binding: ItemExtensionAddBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExtensionAddBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = map.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, isChecked, isInstalled) = map[position]
        val binding = holder.binding
        binding.extensionName.text = if (!isInstalled) item.name
        else binding.root.context.getString(R.string.extension_installed, item.name)
        binding.extensionSubtitle.text = item.subtitle ?: item.id
        binding.itemExtension.apply {
            item.iconUrl?.toImageHolder().loadAsCircle(this, R.drawable.ic_extension) {
                setImageDrawable(it)
            }
        }
        binding.extensionSwitch.isChecked = isChecked
        binding.extensionSwitch.setOnCheckedChangeListener { _, checked ->
            listener.onChecked(item, checked)
        }
        binding.root.setOnClickListener {
            binding.extensionSwitch.isChecked = !binding.extensionSwitch.isChecked
        }
        binding.extensionSwitch.isClickable = false
    }

    class Header(
        val listener: Listener
    ) : RecyclerView.Adapter<Header.ViewHolder>() {

        fun interface Listener {
            fun onClose()
        }

        class ViewHolder(val binding: ItemExtensionAddHeaderBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemExtensionAddHeaderBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount() = 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.root.setOnClickListener { listener.onClose() }
        }

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
