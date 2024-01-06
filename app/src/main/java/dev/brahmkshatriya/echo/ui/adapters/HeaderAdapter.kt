package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
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
        val binding = holder.binding
        binding.topAppBarHeader.setText(header)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_settings -> {
                    true
                }

                R.id.menu_extensions -> {
                    binding.root.findNavController().navigate(R.id.dialog_extension)
                    true
                }

                else -> false
            }
        }
    }
}