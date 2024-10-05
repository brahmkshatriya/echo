package dev.brahmkshatriya.echo.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.databinding.ItemArtistInfoBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemArtistInfoBinding

class ArtistHeaderAdapter(private val listener: Listener) :
    RecyclerView.Adapter<ArtistHeaderAdapter.ViewHolder>() {

    override fun getItemCount() = 1

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class Info(
            val binding: ItemArtistInfoBinding,
            val artist: Artist,
            listener: Listener,
        ) : ViewHolder(binding.root) {
            init {
                binding.artistSubscribe.setOnClickListener {
                    artist.let { it1 -> listener.onSubscribeClicked(it1, true) }
                }
                binding.artistUnsubscribe.setOnClickListener {
                    artist.let { it1 -> listener.onSubscribeClicked(it1, false) }
                }
                binding.artistRadio.setOnClickListener {
                    artist.let { it1 -> listener.onRadioClicked(it1) }
                }
            }
        }

        class ShimmerViewHolder(binding: SkeletonItemArtistInfoBinding) : ViewHolder(binding.root)
    }

    interface Listener {
        fun onSubscribeClicked(artist: Artist, subscribe: Boolean)
        fun onRadioClicked(artist: Artist)
    }

    override fun getItemViewType(position: Int) = if (_artist == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) ViewHolder.ShimmerViewHolder(
            SkeletonItemArtistInfoBinding.inflate(inflater, parent, false)
        )
        else {
            val artist = _artist!!
            ViewHolder.Info(
                ItemArtistInfoBinding.inflate(inflater, parent, false),
                artist,
                listener
            )
        }
    }

    private var _artist: Artist? = null
    private var _hasSubscribe = false
    private var _hasRadio = false
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder !is ViewHolder.Info) return
        val binding = holder.binding
        val artist = holder.artist

        var description = artist.followers?.let {
            binding.root.resources.getQuantityString(R.plurals.number_followers, it, it)
        } ?: ""

        description += artist.description?.let { if (description.isBlank()) it else "\n\n$it" }
        binding.artistDescriptionContainer.isVisible = if (description.isBlank()) false else {
            binding.artistDescription.text = description
            true
        }

        binding.artistDescription.apply {
            setOnClickListener {
                maxLines = if (maxLines == 3) Int.MAX_VALUE else 3
            }
        }

        if (_hasSubscribe) {
            binding.artistSubscribe.isVisible = !artist.isFollowing
            binding.artistUnsubscribe.isVisible = artist.isFollowing
        } else {
            binding.artistSubscribe.isVisible = false
            binding.artistUnsubscribe.isVisible = false
        }
        binding.artistRadio.isVisible = _hasRadio
    }

    fun submit(artist: Artist, hasSubscribe: Boolean, hasRadio: Boolean) {
        _artist = artist
        _hasSubscribe = hasSubscribe
        _hasRadio = hasRadio
        notifyItemChanged(0)
    }

}