package dev.brahmkshatriya.echo.ui.media.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemMediaHeaderBinding
import dev.brahmkshatriya.echo.ui.media.MediaViewModel
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ui.SimpleItemSpan
import dev.brahmkshatriya.echo.utils.ui.UiUtils.toTimeString
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MediaHeaderAdapter(
    private val listener: Listener,
) : RecyclerView.Adapter<MediaHeaderAdapter.ViewHolder>() {

    interface Listener {
        fun onFollowClicked(id: String?, item: EchoMediaItem?, view: View)
        fun onUnfollowClicked(id: String?, item: EchoMediaItem?, view: View)
        fun onTextClicked(id: String?, item: EchoMediaItem?, view: View)
        fun openMediaItem(id: String?, item: EchoMediaItem?)
        fun onRadioClicked(id: String?, item: EchoMediaItem?, view: View)
        fun onShuffleClicked(id: String?, item: EchoMediaItem?, view: View)
        fun onPlayClicked(id: String?, item: EchoMediaItem?, view: View)
    }

    var clickEnabled = true

    inner class ViewHolder(
        val binding: ItemMediaHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                follow.setOnClickListener { listener.onFollowClicked(extensionId, item, it) }
                unfollow.setOnClickListener { listener.onUnfollowClicked(extensionId, item, it) }
                text.movementMethod = LinkMovementMethod.getInstance()
                text.setOnClickListener {
                    if (clickEnabled) listener.onTextClicked(extensionId, item, it)
                }
                radio.setOnClickListener { listener.onRadioClicked(extensionId, item, it) }
                shuffle.setOnClickListener { listener.onShuffleClicked(extensionId, item, it) }
                play.setOnClickListener { listener.onPlayClicked(extensionId, item, it) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemMediaHeaderBinding.inflate(inflater, parent, false))
    }

    private var extensionId: String? = null
    private var item: EchoMediaItem? = null
    private var isRadioSupported = false
    private var isFollowSupported = false

    @SuppressLint("NotifyDataSetChanged")
    fun submit(id: String?, item: EchoMediaItem?, client: ExtensionClient?) {
        this.item = item
        this.extensionId = id
        this.isRadioSupported = client is RadioClient
        this.isFollowSupported =
            client is ArtistFollowClient && item is EchoMediaItem.Profile.ArtistItem
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (item != null) 1 else 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = item ?: return
        holder.binding.bind(item)
    }

    private fun ItemMediaHeaderBinding.bind(item: EchoMediaItem) {
        radio.isVisible = isRadioSupported && item !is EchoMediaItem.Lists.RadioItem
        shuffle.isVisible = item !is EchoMediaItem.TrackItem
        val isFollowing = item is EchoMediaItem.Profile.ArtistItem && item.artist.isFollowing
        follow.isVisible = !isFollowing && isFollowSupported
        unfollow.isVisible = isFollowing && isFollowSupported
        text.text = root.context.getSpan(true, item, extensionId) { a, b ->
            clickEnabled = false
            listener.openMediaItem(a, b)
            text.post { clickEnabled = true }
        }
        text.isVisible = !text.text.isNullOrBlank()
    }

    companion object {
        private const val MAX_DESC_TEXT = 128
        private fun String.ellipsize() = if (length > MAX_DESC_TEXT) {
            substring(0, MAX_DESC_TEXT) + "..."
        } else this


        fun Context.getSpan(
            compact: Boolean,
            item: EchoMediaItem,
            extensionId: String?,
            openMediaItem: (String?, EchoMediaItem?) -> Unit
        ) = when (item) {
            is EchoMediaItem.Lists.AlbumItem -> {
                val album = item.album
                val span = SpannableString(buildString {
                    if (album.isExplicit) {
                        append("\uD83C\uDD74 ")
                        appendLine(getString(R.string.explicit))
                    }
                    val tracks = album.tracks
                    if (tracks != null) append(
                        runCatching {
                            resources.getQuantityString(R.plurals.n_songs, tracks, tracks)
                        }.getOrNull() ?: getString(R.string.x_songs, tracks)
                    )

                    if (album.releaseDate != null) {
                        if (tracks != null) append(" • ") else appendLine()
                        append(album.releaseDate.toString())
                    }
                    if (album.label != null) {
                        if (album.releaseDate != null || tracks != null) appendLine()
                        append(album.label)
                    }
                    if (album.artists.isNotEmpty()) {
                        if (album.releaseDate != null || tracks != null || album.label != null) {
                            appendLine()
                        }
                        append(
                            getString(
                                R.string.by_x,
                                album.artists.joinToString(", ") { it.name }
                            )
                        )
                    }
                    if (!album.description.isNullOrBlank()) {
                        if (album.releaseDate != null || tracks != null || album.label != null || album.artists.isNotEmpty()) {
                            appendLine()
                            appendLine()
                        }
                        append(
                            if (compact) album.description?.ellipsize()
                            else album.description
                        )
                    }
                }.trimEnd('\n'))
                album.artists.forEach { artist ->
                    val start = span.indexOf(artist.name)
                    val end = start + artist.name.length
                    val clickableSpan = SimpleItemSpan(this) {
                        openMediaItem(extensionId, artist.toMediaItem())
                    }
                    span.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                span
            }

            is EchoMediaItem.Lists.PlaylistItem -> {
                val playlist = item.playlist
                val span = SpannableString(buildString {
                    val tracks = playlist.tracks
                    if (tracks != null) {
                        runCatching {
                            resources.getQuantityString(R.plurals.n_songs, tracks, tracks)
                        }.getOrNull() ?: getString(R.string.x_songs, tracks)
                    }
                    if (playlist.creationDate != null) {
                        if (tracks != null) append(" • ") else appendLine()
                        append(playlist.creationDate.toString())
                    }
                    if (playlist.duration != null) {
                        if (playlist.creationDate != null) {
                            appendLine()
                        }
                        append(playlist.duration?.toTimeString())
                    }
                    if (playlist.authors.isNotEmpty()) {
                        if (playlist.creationDate != null || playlist.duration != null) {
                            appendLine()
                        }
                        append(
                            getString(
                                R.string.by_x,
                                playlist.authors.joinToString(", ") { it.name }
                            )
                        )
                    }
                    if (playlist.description != null) {
                        if (playlist.creationDate != null || playlist.duration != null || playlist.authors.isNotEmpty()) {
                            appendLine()
                            appendLine()
                        }
                        append(
                            if (compact) playlist.description?.ellipsize()
                            else playlist.description
                        )
                    }
                }.trimEnd('\n'))
                playlist.authors.forEach { author ->
                    val start = span.indexOf(author.name)
                    val end = start + author.name.length
                    val clickableSpan = SimpleItemSpan(this) {
                        openMediaItem(extensionId, author.toMediaItem())
                    }
                    span.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                span
            }

            is EchoMediaItem.Lists.RadioItem -> SpannableString("")
            is EchoMediaItem.Profile.ArtistItem -> {
                val artist = item.artist
                SpannableString(buildString {
                    if (artist.followers != null) {
                        val formatter = CompactDecimalFormat.getInstance()
                        val formatted = formatter.format(artist.followers)
                        append(getString(R.string.x_followers, formatted))
                    }
                    if (artist.description != null) {
                        if (artist.followers != null) {
                            appendLine()
                            appendLine()
                        }
                        append(
                            if (compact) artist.description?.ellipsize()
                            else artist.description
                        )
                    }
                }.trimEnd('\n'))
            }

            is EchoMediaItem.Profile.UserItem -> SpannableString("")
            is EchoMediaItem.TrackItem -> SpannableString(item.track.getInfoString(compact, this))
        }

        fun Track.getInfoString(
            compact: Boolean,
            context: Context,
        ): String {
            return buildString {
                if (isExplicit) {
                    append("\uD83C\uDD74 ")
                    append(context.getString(R.string.explicit))
                }
                if (duration != null) {
                    appendLine()
                    append(duration?.toTimeString())
                }
                if (releaseDate != null) {
                    if (duration != null) append(" • ") else appendLine()
                    append(releaseDate?.toString())
                }
                if (plays != null) {
                    val formatter = CompactDecimalFormat.getInstance()
                    val formatted = formatter.format(plays)
                    appendLine()
                    append(context.getString(R.string.x_plays, formatted))
                }
                if (description != null) {
                    if (duration != null || releaseDate != null || plays != null) {
                        appendLine()
                        appendLine()
                    }
                    append(
                        if (compact) description?.ellipsize()
                        else description
                    )
                }
            }.trimEnd('\n')
        }

        fun getListener(
            fragment: Fragment,
            openMediaItem: (String?, EchoMediaItem?) -> Unit
        ): Listener {
            val playerVM by fragment.activityViewModel<PlayerViewModel>()
            val vm by fragment.viewModel<MediaViewModel>()
            return object : Listener {
                override fun onFollowClicked(id: String?, item: EchoMediaItem?, view: View) {
                    val artist = item as? EchoMediaItem.Profile.ArtistItem ?: return
                    vm.follow(artist.artist, true)
                }

                override fun onUnfollowClicked(id: String?, item: EchoMediaItem?, view: View) {
                    val artist = item as? EchoMediaItem.Profile.ArtistItem ?: return
                    vm.follow(artist.artist, false)
                }

                override fun onTextClicked(id: String?, item: EchoMediaItem?, view: View) {
                    item ?: return
                    id ?: return
                    val context = fragment.requireContext()
                    var dialog: AlertDialog? = null
                    val builder = MaterialAlertDialogBuilder(context)
                        .setTitle(item.title)
                        .setMessage(context.getSpan(false, item, id) { m, n ->
                            openMediaItem(m, n)
                            dialog?.dismiss()
                        })
                        .setPositiveButton(fragment.getString(R.string.okay)) { d, _ ->
                            d.dismiss()
                        }
                    dialog = builder.create()
                    dialog.show()
                    val text = dialog.findViewById<TextView>(android.R.id.message)!!
                    text.movementMethod = LinkMovementMethod.getInstance()
                }

                override fun openMediaItem(id: String?, item: EchoMediaItem?) {
                    openMediaItem(id, item)
                }

                override fun onRadioClicked(id: String?, item: EchoMediaItem?, view: View) {
                    id ?: return
                    item ?: return
                    playerVM.radio(id, item)
                }

                override fun onShuffleClicked(id: String?, item: EchoMediaItem?, view: View) {
                    id ?: return
                    item ?: return
                    playerVM.shuffle(id, item, !vm.loadingFlow.value)
                }

                override fun onPlayClicked(id: String?, item: EchoMediaItem?, view: View) {
                    id ?: return
                    item ?: return
                    playerVM.play(id, item, !vm.loadingFlow.value)
                }
            }
        }
    }
}