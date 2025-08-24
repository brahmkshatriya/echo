package dev.brahmkshatriya.echo.ui.media

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
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Album.Type.Book
import dev.brahmkshatriya.echo.common.models.Album.Type.Compilation
import dev.brahmkshatriya.echo.common.models.Album.Type.EP
import dev.brahmkshatriya.echo.common.models.Album.Type.LP
import dev.brahmkshatriya.echo.common.models.Album.Type.PreRelease
import dev.brahmkshatriya.echo.common.models.Album.Type.Show
import dev.brahmkshatriya.echo.common.models.Album.Type.Single
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemLineBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaHeaderBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfErrorBinding
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils.getFinalTitle
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils.getMessage
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.media.MediaFragment.Companion.getBundle
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ui.SimpleItemSpan
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.toTimeString
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class MediaHeaderAdapter(
    private val listener: Listener,
    private val fromPlayer: Boolean,
) : ScrollAnimRecyclerAdapter<MediaHeaderAdapter.ViewHolder>(), GridAdapter {

    interface Listener {
        fun onRetry(view: View)
        fun onError(view: View, error: Throwable?)
        fun onDescriptionClicked(view: View, extensionId: String?, item: EchoMediaItem?)
        fun openMediaItem(extensionId: String, item: EchoMediaItem)
        fun onFollowClicked(view: View, follow: Boolean)
        fun onSavedClicked(view: View, saved: Boolean)
        fun onLikeClicked(view: View, liked: Boolean)
        fun onPlayClicked(view: View)
        fun onRadioClicked(view: View)
        fun onShareClicked(view: View)
        fun onHideClicked(view: View, hidden: Boolean)
    }

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> Success(parent, listener, fromPlayer)
        1 -> Error(parent, listener)
        2 -> Loading(parent)
        else -> throw IllegalArgumentException("Unknown view type: $viewType")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (holder) {
            is Success -> {
                val state = result?.getOrNull() ?: return
                holder.bind(state)
            }

            is Error -> {
                val error = result?.exceptionOrNull() ?: return
                holder.bind(error)
            }

            is Loading -> {}
        }
    }

    override fun getItemCount() = 1
    override fun getItemViewType(position: Int) = when (result?.isSuccess) {
        true -> 0
        false -> 1
        null -> 2
    }

    var result: Result<MediaState.Loaded<*>>? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    sealed class ViewHolder(itemView: View) : ScrollAnimViewHolder(itemView)
    class Success(
        parent: ViewGroup,
        private val listener: Listener,
        private val fromPlayer: Boolean,
        private val binding: ItemMediaHeaderBinding = ItemMediaHeaderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : ViewHolder(binding.root) {
        val buttons = binding.run {
            listOf(
                followButton, playButton, savedButton, likeButton, hideButton,
                radioButton, shareButton
            )
        }

        init {
            binding.followButton.setOnClickListener {
                listener.onFollowClicked(it, binding.followButton.isChecked)
                it.isEnabled = false
            }
            binding.savedButton.setOnClickListener {
                listener.onSavedClicked(it, binding.savedButton.isChecked)
                it.isEnabled = false
            }
            binding.likeButton.setOnClickListener {
                listener.onLikeClicked(it, binding.likeButton.isChecked)
                it.isEnabled = false
            }
            binding.hideButton.setOnClickListener {
                listener.onHideClicked(it, binding.hideButton.isChecked)
                it.isEnabled = false
            }
            binding.playButton.setOnClickListener {
                listener.onPlayClicked(it)
            }
            binding.radioButton.setOnClickListener {
                listener.onRadioClicked(it)
            }
            binding.shareButton.setOnClickListener {
                listener.onShareClicked(it)
                it.isEnabled = false
            }
        }


        fun configureButtons() {
            val visible = buttons.filter { it.isVisible }
            binding.buttonGroup.isVisible = visible.isNotEmpty()
            val isNotOne = visible.size > 1
            visible.forEachIndexed { index, button ->
                button.isEnabled = true
                if (index == 0 && isNotOne) button.run {
                    updatePaddingRelative(
                        start = if (icon != null) 16.dpToPx(context) else 24.dpToPx(context),
                        end = 24.dpToPx(context)
                    )
                    iconPadding = 8.dpToPx(context)
                    text = contentDescription
                } else button.run {
                    updatePaddingRelative(start = 12.dpToPx(context), end = 12.dpToPx(context))
                    iconPadding = 0
                    text = null
                }
            }
            binding.buttonGroup.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = if (isNotOne) ViewGroup.LayoutParams.MATCH_PARENT
                else ViewGroup.LayoutParams.WRAP_CONTENT
                bottomMargin = if (isNotOne) 0 else (-56).dpToPx(binding.root.context)
            }
            binding.description.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = if (isNotOne) 0 else 48.dpToPx(binding.root.context)
            }
        }

        var clickEnabled = true
        var state: MediaState.Loaded<*>? = null

        fun bind(state: MediaState.Loaded<*>) = with(binding) {
            this@Success.state = state
            followButton.isVisible = state.isFollowed != null
            followButton.isChecked = state.isFollowed ?: false
            followButton.contentDescription = root.context.getString(
                if (state.isFollowed == true) R.string.unfollow else R.string.follow
            )

            savedButton.isVisible = state.isSaved != null
            savedButton.isChecked = state.isSaved ?: false
            savedButton.contentDescription = root.context.getString(
                if (state.isSaved == true) R.string.unsave else R.string.save
            )

            likeButton.isVisible = state.isLiked != null && !fromPlayer
            likeButton.isChecked = state.isLiked ?: false
            likeButton.contentDescription = root.context.getString(
                if (state.isLiked == true) R.string.unlike else R.string.like
            )

            hideButton.isVisible = state.isHidden != null
            hideButton.isChecked = state.isHidden ?: false
            hideButton.contentDescription = root.context.getString(
                if (state.isHidden == true) R.string.unhide else R.string.hide
            )

            playButton.isVisible = state.item is Track && !fromPlayer && state.item.isPlayable == Track.Playable.Yes
            radioButton.isVisible = state.showRadio
            shareButton.isVisible = state.showShare
            configureButtons()

            explicit.isVisible = state.item.isExplicit
            followers.isVisible = state.followers != null
            followers.text = state.followers?.let {
                val formatter = CompactDecimalFormat.getInstance()
                root.context.getString(R.string.x_followers, formatter.format(it))
            }
            val span =
                root.context.getSpan(true, state.extensionId, state.item) { id, item ->
                    clickEnabled = false
                    listener.openMediaItem(id, item)
                    description.post { clickEnabled = true }
                }
            description.text = span
            description.isVisible = span.isNotEmpty()
        }

        init {
            binding.run {
                description.movementMethod = LinkMovementMethod.getInstance()
                description.setOnClickListener {
                    if (clickEnabled) listener.onDescriptionClicked(
                        it,
                        state?.extensionId,
                        state?.item
                    )
                }
            }
        }
    }

    class Error(
        parent: ViewGroup,
        listener: Listener,
        private val binding: ItemShelfErrorBinding = ItemShelfErrorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : ViewHolder(binding.root) {
        var throwable: Throwable? = null

        init {
            binding.errorView.setOnClickListener {
                listener.onError(binding.error, throwable)
            }
            binding.retry.setOnClickListener {
                listener.onRetry(it)
            }
        }

        fun bind(throwable: Throwable) {
            this.throwable = throwable
            binding.error.run {
                transitionName = throwable.hashCode().toString()
                text = context.getFinalTitle(throwable)
            }
        }
    }

    class Loading(
        parent: ViewGroup,
        binding: ItemLineBinding = ItemLineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : ViewHolder(binding.root) {
        init {
            itemView.alpha = 0f
        }
    }

    companion object {
        private const val MAX_DESC_TEXT = 144
        private fun String.ellipsize() = if (length > MAX_DESC_TEXT) {
            substring(0, MAX_DESC_TEXT) + "..."
        } else this

        private const val DIVIDER = " • "
        fun Context.getSpan(
            compact: Boolean,
            extensionId: String,
            item: EchoMediaItem,
            openMediaItem: (String, EchoMediaItem) -> Unit = { a, b -> },
        ): SpannableString = when (item) {
            is EchoMediaItem.Lists -> {
                val madeBy = item.artists.joinToString(", ") { it.name }
                val span = SpannableString(buildString {
                    val firstRow = listOfNotNull(
                        getString(item.typeInt),
                        item.date?.toString(),
                    ).joinToString(DIVIDER)
                    val secondRow = listOfNotNull(
                        item.toTrackString(this@getSpan),
                        item.duration?.toTimeString()
                    ).joinToString(DIVIDER)
                    if (firstRow.isNotEmpty()) appendLine(firstRow)
                    if (secondRow.isNotEmpty()) appendLine(secondRow)
                    val desc = item.description
                    if (desc != null) {
                        appendLine()
                        appendLine(if (compact) desc.ellipsize() else desc)
                    }
                    if (madeBy.isNotEmpty()) {
                        appendLine()
                        appendLine(getString(R.string.by_x, madeBy))
                    }
                    if (item.label != null) {
                        appendLine()
                        appendLine(item.label)
                    }
                }.trimEnd('\n').trimStart('\n'))
                val madeByIndex = span.indexOf(madeBy)
                item.artists.forEach {
                    val start = span.indexOf(it.name, madeByIndex)
                    if (start != -1) {
                        val end = start + it.name.length
                        val clickableSpan = SimpleItemSpan(this) {
                            openMediaItem(extensionId, it)
                        }
                        span.setSpan(
                            clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                span
            }

            is Artist -> {
                val desc = if (compact) item.bio?.ellipsize() else item.bio
                SpannableString(desc ?: "")
            }

            is Track -> {
                SpannableString(buildString {
                    val firstRow = listOfNotNull(
                        getString(
                            when (item.type) {
                                Track.Type.Song, Track.Type.VideoSong -> R.string.song
                                Track.Type.Video, Track.Type.HorizontalVideo -> R.string.video
                                Track.Type.Podcast -> R.string.podcast
                            }
                        ),
                        item.releaseDate
                    ).joinToString(DIVIDER)
                    val secondRow = listOfNotNull(
                        item.duration?.toTimeString(),
                        if (item.plays != null) {
                            val formatter = CompactDecimalFormat.getInstance()
                            getString(R.string.x_plays, formatter.format(item.plays))
                        } else null
                    ).joinToString(DIVIDER)
                    if (firstRow.isNotEmpty()) appendLine(firstRow)
                    if (secondRow.isNotEmpty()) appendLine(secondRow)
                    val notPlayable = item.playableString(this@getSpan)
                    if (!notPlayable.isNullOrEmpty()) {
                        appendLine()
                        appendLine(notPlayable)
                    }
                    val desc = item.description
                    if (desc != null) {
                        appendLine()
                        appendLine(if (compact) desc.ellipsize() else desc)
                        appendLine()
                    }
                    val genres = item.genres.joinToString(", ")
                    if (genres.isNotEmpty()) {
                        appendLine(getString(R.string.genres_x, genres))
                    }
                    val isrc = item.isrc
                    if (isrc != null) {
                        appendLine(getString(R.string.isrc_x, isrc))
                    }
                    val label = item.album?.label
                    if (label != null) {
                        appendLine()
                        appendLine(label)
                    }
                    val lastRow = listOfNotNull(
                        item.albumDiscNumber?.let {
                            getString(R.string.disc_number_n, it)
                        },
                        item.albumOrderNumber?.let {
                            getString(R.string.album_order_n, it)
                        }
                    ).joinToString(DIVIDER)
                    if (lastRow.isNotEmpty()) {
                        appendLine()
                        appendLine(lastRow)
                    }
                }.trimStart('\n').trimEnd('\n'))
            }
        }

        fun Context.unfuckedString(
            numberStringId: Int, nStringId: Int, count: Int,
        ) = runCatching {
            resources.getQuantityString(numberStringId, count, count)
        }.getOrNull() ?: getString(nStringId, count)

        fun Fragment.getMediaHeaderListener(viewModel: MediaDetailsViewModel) = object : Listener {
            override fun onRetry(view: View) {
                viewModel.refresh()
            }

            override fun onError(view: View, error: Throwable?) {
                error ?: return
                requireActivity().getMessage(error, view).action?.handler?.invoke()
            }

            override fun openMediaItem(extensionId: String, item: EchoMediaItem) {
                openFragment<MediaFragment>(null, getBundle(extensionId, item, false))
            }

            override fun onFollowClicked(view: View, follow: Boolean) {
                viewModel.followItem(follow)
            }

            override fun onSavedClicked(view: View, saved: Boolean) {
                viewModel.saveToLibrary(saved)
            }

            override fun onLikeClicked(view: View, liked: Boolean) {
                viewModel.likeItem(liked)
            }

            override fun onHideClicked(view: View, hidden: Boolean) {
                viewModel.hideItem(hidden)
            }

            override fun onPlayClicked(view: View) {
                val (extensionId, item, loaded) = viewModel.getItem() ?: return
                val vm by activityViewModels<PlayerViewModel>()
                vm.play(extensionId, item, loaded)
            }

            override fun onRadioClicked(view: View) {
                val (extensionId, item, loaded) = viewModel.getItem() ?: return
                val vm by activityViewModels<PlayerViewModel>()
                vm.radio(extensionId, item, loaded)
            }

            override fun onShareClicked(view: View) {
                viewModel.onShare()
            }

            override fun onDescriptionClicked(
                view: View, extensionId: String?, item: EchoMediaItem?,
            ) {
                item ?: return
                extensionId ?: return
                val context = requireContext()
                var dialog: AlertDialog? = null
                val builder = MaterialAlertDialogBuilder(context)
                builder.setTitle(item.title)
                builder.setMessage(context.getSpan(false, extensionId, item) { m, n ->
                    openMediaItem(m, n)
                    dialog?.dismiss()
                })
                builder.setPositiveButton(getString(R.string.okay)) { d, _ ->
                    d.dismiss()
                }
                dialog = builder.create()
                dialog.show()
                val text = dialog.findViewById<TextView>(android.R.id.message)!!
                text.movementMethod = LinkMovementMethod.getInstance()
            }
        }

        val EchoMediaItem.Lists.typeInt
            get() = when (this) {
                is Album -> when (type) {
                    PreRelease -> R.string.pre_release
                    Single -> R.string.single
                    EP -> R.string.ep
                    LP -> R.string.lp
                    Compilation -> R.string.compilation
                    Show -> R.string.show
                    Book -> R.string.book
                    null -> R.string.album
                }

                is Playlist -> R.string.playlist
                is Radio -> R.string.radio
            }

        fun EchoMediaItem.Lists.toTrackString(context: Context) = context.run {
            val tracks = trackCount?.toInt()
            if (tracks != null) {
                when (type) {
                    PreRelease, Single, EP, LP, Compilation -> unfuckedString(
                        R.plurals.number_songs, R.string.n_songs, tracks
                    )

                    Show -> unfuckedString(
                        R.plurals.number_episodes, R.string.n_episodes, tracks
                    )

                    Book -> unfuckedString(
                        R.plurals.number_chapters, R.string.n_chapters, tracks
                    )

                    null -> unfuckedString(
                        R.plurals.number_tracks, R.string.n_tracks, tracks
                    )
                }
            } else null
        }

        fun Track.playableString(context: Context) = when (val play = isPlayable) {
            is Track.Playable.No -> context.getString(R.string.not_playable_x, play.reason)
            Track.Playable.Yes -> null
            Track.Playable.RegionLocked -> context.getString(R.string.unavailable_in_your_region)
            Track.Playable.Unreleased -> if (releaseDate != null) context.getString(
                R.string.releases_on_x, releaseDate.toString()
            ) else context.getString(R.string.not_yet_released)
        }
    }
}