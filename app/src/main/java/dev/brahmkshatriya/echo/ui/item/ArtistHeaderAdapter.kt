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
            adapter: ArtistHeaderAdapter,
        ) : ViewHolder(binding.root) {
            init {
                binding.artistSubscribe.setOnClickListener {
                    artist.let { it1 -> listener.onSubscribeClicked(it1, true, adapter) }
                }
                binding.artistUnsubscribe.setOnClickListener {
                    artist.let { it1 -> listener.onSubscribeClicked(it1, false, adapter) }
                }
                binding.artistRadio.setOnClickListener {
                    artist.let { it1 -> listener.onRadioClicked(it1) }
                }
            }
        }

        class ShimmerViewHolder(binding: SkeletonItemArtistInfoBinding) : ViewHolder(binding.root)
    }

    interface Listener {
        fun onSubscribeClicked(artist: Artist, subscribe: Boolean, adapter: ArtistHeaderAdapter)
        fun onRadioClicked(artist: Artist)
    }

    override fun getItemViewType(position: Int) = if (_artist == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            ViewHolder.ShimmerViewHolder(
                SkeletonItemArtistInfoBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        } else {
            val artist = _artist!!
            ViewHolder.Info(
                ItemArtistInfoBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), artist, listener, this
            )
        }
    }

    private var _artist: Artist? = null
    private var isSubscribed = false
    private var _subscribe = false
    private var _radio = false
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder !is ViewHolder.Info) return
        val binding = holder.binding
        val artist = holder.artist

        binding.artistSubtitle.isVisible = artist.followers?.let {
            binding.artistSubtitle.text = binding.artistSubtitle.resources.getQuantityString(
                R.plurals.number_followers, it, it
            )
            true
        } ?: false

        binding.artistDescriptionContainer.isVisible = artist.description?.let {
            if (it.isNotBlank()) {
                binding.artistDescription.text = it
                true
            } else false
        } ?: false

        if(_subscribe) {
            binding.artistSubscribe.isVisible = !isSubscribed
            binding.artistUnsubscribe.isVisible = isSubscribed
        } else {
            binding.artistSubscribe.isVisible = false
            binding.artistUnsubscribe.isVisible = false
        }
        binding.artistRadio.isVisible = _radio
    }

    fun submit(artist: Artist, subscribe: Boolean, radio: Boolean) {
        _artist = artist
        _subscribe = subscribe
        _radio = radio
        notifyItemChanged(0)
    }

    fun submitSubscribe(subscribe: Boolean) {
        isSubscribed = subscribe
        notifyItemChanged(0)
    }
}