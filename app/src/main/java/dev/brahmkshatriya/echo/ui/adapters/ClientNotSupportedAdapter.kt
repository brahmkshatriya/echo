package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ItemClientNotSupportedBinding

class ClientNotSupportedAdapter(
    private val clientStringId: Int
) : RecyclerView.Adapter<ClientNotSupportedAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    class ViewHolder(val binding: ItemClientNotSupportedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemClientNotSupportedBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val clientName = binding.root.context.getString(clientStringId)
        binding.notSupportedTextView.text =
            binding.root.context.getString(R.string.is_not_supported, clientName)
    }
}