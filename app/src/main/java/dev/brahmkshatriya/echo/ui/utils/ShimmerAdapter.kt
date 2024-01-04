package dev.brahmkshatriya.echo.ui.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.SkeletonItemAlbumRecyclerBinding

class ShimmerAdapter : RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder>() {
    class ShimmerViewHolder(binding: SkeletonItemAlbumRecyclerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val binding =
            SkeletonItemAlbumRecyclerBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        return ShimmerViewHolder(binding)
    }

    override fun getItemCount(): Int = 10

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {}
}