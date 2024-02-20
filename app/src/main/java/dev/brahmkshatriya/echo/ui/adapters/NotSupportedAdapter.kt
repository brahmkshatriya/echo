package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ItemNotSupportedBinding

class NotSupportedAdapter(
    private val clientStringId: Int
) : RecyclerView.Adapter<NotSupportedAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    class ViewHolder(val binding: ItemNotSupportedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemNotSupportedBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
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
        val clientName = binding.root.context.getString(clientStringId)
        binding.notSupportedTextView.text =
            binding.root.context.getString(R.string.is_not_supported, clientName)
    }
}