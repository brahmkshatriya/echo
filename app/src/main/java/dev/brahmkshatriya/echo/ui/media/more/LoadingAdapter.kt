package dev.brahmkshatriya.echo.ui.media.more

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemLoadingBinding

class LoadingAdapter : RecyclerView.Adapter<LoadingAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemLoadingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLoadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.root.updateLayoutParams { height = WRAP_CONTENT }
        return ViewHolder(binding)
    }

    private var loading = false

    @SuppressLint("NotifyDataSetChanged")
    fun setLoading(loading: Boolean) {
        this.loading = loading
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (loading) 1 else 0
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
}