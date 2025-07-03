package dev.brahmkshatriya.echo.ui.player.quality

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.databinding.DialogPlayerQualitySelectionBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.serverIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.serverWithDownloads
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.main.settings.AudioFragment
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.player.quality.FormatUtils.getDetails
import dev.brahmkshatriya.echo.ui.player.quality.FormatUtils.getSelected
import dev.brahmkshatriya.echo.ui.player.quality.FormatUtils.toAudioDetails
import dev.brahmkshatriya.echo.ui.player.quality.FormatUtils.toSubtitleDetails
import dev.brahmkshatriya.echo.ui.player.quality.FormatUtils.toVideoDetails
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class QualitySelectionBottomSheet : BottomSheetDialogFragment() {
    var binding by autoCleared<DialogPlayerQualitySelectionBinding>()
    private val viewModel by activityViewModel<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPlayerQualitySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.topAppBar.setOnMenuItemClickListener {
            dismiss()
            requireActivity().openFragment<AudioFragment>()
            true
        }

        observe(viewModel.buffering) {
            binding.progressIndicator.isVisible = it
        }
        observe(viewModel.playerState.current) { current ->
            val item = current?.mediaItem ?: return@observe
            val track = item.track
            binding.run {
                applyChips(
                    item.serverWithDownloads(requireContext()),
                    streamableServer, streamableServerGroup, item.serverIndex
                ) {
                    it ?: return@applyChips
                    viewModel.changeServer(it)
                }
                streamableServer.isVisible = true
                streamableServerGroup.isVisible = true

                applyChips(
                    listOf(null, *track.backgrounds.toTypedArray()), streamableBackgrounds,
                    streamableBackgroundGroup, item.backgroundIndex + 1
                ) {
                    viewModel.changeBackground(it)
                }

                applyChips(
                    listOf(null, *track.subtitles.toTypedArray()), streamableSubtitles,
                    streamableSubtitleGroup, item.subtitleIndex + 1
                ) {
                    viewModel.changeSubtitle(it)
                }
            }
        }
        fun applyServer() {
            val servers = viewModel.playerState.servers
            val item = viewModel.playerState.current.value?.mediaItem
            val server = servers[item?.mediaId]?.getOrNull()
            val list = if (server != null && !server.merged) server.sources else listOf()
            binding.run {
                applyChips(
                    list, streamableSource, streamableSourceGroup, item?.sourceIndex,
                    { viewModel.changeCurrentSource(list.indexOf(it)) }) {
                    it.title ?: getString(R.string.quality_x, it.quality)
                }
            }
        }
        applyServer()
        observe(viewModel.playerState.serverChanged) { applyServer() }

        observe(viewModel.tracks) { tracks ->
            val details = tracks?.getDetails(requireContext())?.joinToString("\n")
            binding.streamableInfo.text = details
            binding.streamableInfo.isVisible = !details.isNullOrBlank()

            val audios = tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO }
            val videos = tracks?.groups?.filter { it.type == C.TRACK_TYPE_VIDEO }
            val subtitles = tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT }

            val onClick: Chip.(Pair<Tracks.Group, Int>?) -> Unit = {
                val trackGroup = it!!.first.mediaTrackGroup
                viewModel.changeTrackSelection(trackGroup, it.second)
            }

            binding.run {
                applyChips(audios, trackAudios, trackAudiosGroup, onClick) {
                    it.toAudioDetails()
                }
                applyChips(videos, trackVideos, trackVideosGroup, onClick) {
                    it.toVideoDetails()
                }
                applyChips(subtitles, trackSubtitles, trackSubtitlesGroup, onClick) {
                    it.toSubtitleDetails()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun applyChips(
        groups: List<Tracks.Group>?,
        textView: TextView,
        chipGroup: ChipGroup,
        onClick: Chip.(Pair<Tracks.Group, Int>?) -> Unit,
        text: (Format) -> String,
    ) {
        val (trackGroups, select) = groups.orEmpty().getSelected()
        applyChips(trackGroups, textView, chipGroup, select, onClick) {
            val format = it.first.getTrackFormat(it.second)
            text(format)
        }
    }

    private fun applyChips(
        streamables: List<Streamable?>,
        textView: TextView,
        chipGroup: ChipGroup,
        selected: Int?,
        onClick: Chip.(Streamable?) -> Unit
    ) {
        val context = chipGroup.context
        applyChips(streamables, textView, chipGroup, selected, onClick) {
            it?.let {
                it.title ?: when (it.type) {
                    Streamable.MediaType.Subtitle -> context.getString(R.string.unknown)
                    else -> context.getString(R.string.quality_x, it.quality)
                }
            } ?: context.getString(R.string.off)
        }
    }

    private fun <T> applyChips(
        list: List<T>,
        textView: TextView,
        chipGroup: ChipGroup,
        selected: Int?,
        onClick: Chip.(T) -> Unit,
        text: (T) -> String,
    ) {
        val visible = list.size > 1
        textView.isVisible = visible
        chipGroup.apply {
            isVisible = visible
            removeAllViews()
            list.forEachIndexed { index, t ->
                val chip = Chip(context)
                chip.text = text(t)
                chip.ellipsize = TextUtils.TruncateAt.MIDDLE
                chip.isCheckable = true
                addView(chip)
                if (index == selected) check(chip.id)
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) onClick(chip, t)
                }
            }
        }
    }
}