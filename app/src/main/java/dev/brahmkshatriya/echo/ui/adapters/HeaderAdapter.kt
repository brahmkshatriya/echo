package dev.brahmkshatriya.echo.ui.adapters

import android.content.Intent
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.databinding.ItemMainHeaderBinding
import dev.brahmkshatriya.echo.ui.extension.ExtensionDialogFragmentDirections
import dev.brahmkshatriya.echo.ui.settings.SettingsActivity

class HeaderAdapter(
    private val header: Int,
    private val chipListener: ((HeaderAdapter, Genre) -> Unit)? = null
) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    class HeaderViewHolder(val binding: ItemMainHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HeaderViewHolder(
        ItemMainHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        val binding = holder.binding
        binding.topAppBarHeader.setText(header)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_settings -> {
                    val intent = Intent(binding.root.context, SettingsActivity::class.java)
                    binding.root.context.startActivity(intent)
                    true
                }

                R.id.menu_extensions -> {
                    val action = ExtensionDialogFragmentDirections.actionExtension()
                    binding.root.findNavController().navigate(action)
                    true
                }

                else -> false
            }
        }
        binding.chipRecyclerView.apply {
            adapter = ChipAdapter {
                chipListener?.invoke(this@HeaderAdapter, it)
            }.apply {
                submitList(chips)
            }

            savedState?.let {
                layoutManager?.onRestoreInstanceState(it)
                savedState = null
            }
            isVisible = chips.isNotEmpty()
        }
    }

    private var savedState: Parcelable? = null

    override fun onViewRecycled(holder: HeaderViewHolder) {
        super.onViewRecycled(holder)
        savedState = holder.binding.chipRecyclerView.layoutManager?.onSaveInstanceState()
    }

    private var chips: List<Pair<Boolean, Genre>> = emptyList()
    fun submitChips(chips: List<Pair<Boolean, Genre>>) {
        this.chips = chips
        notifyItemChanged(0)
    }
}