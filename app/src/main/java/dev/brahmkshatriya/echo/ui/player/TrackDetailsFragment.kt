package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingData
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentTrackDetailsBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoIndex
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.adapter.MediaClickListener
import dev.brahmkshatriya.echo.ui.adapter.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class TrackDetailsFragment : Fragment() {
    private var binding by autoCleared<FragmentTrackDetailsBinding>()
    private val playerViewModel by activityViewModels<PlayerViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()
    private val viewModel by activityViewModels<TrackDetailsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mediaClickListener = MediaClickListener(requireActivity().supportFragmentManager) {
            uiViewModel.collapsePlayer()
        }

        var mediaAdapter: MediaContainerAdapter? = null
        println("created ${playerViewModel.currentFlow.value}")
        observe(playerViewModel.currentFlow) { current ->
            println("item : ${current?.mediaItem?.isLoaded}")
            val (_, item) = current ?: return@observe
            item.takeIf { it.isLoaded } ?: return@observe

            val track = item.track
            println("track : $track")
            var desc = ""
            track.plays?.let { desc += getString(R.string.track_total_plays, it) }
            track.releaseDate?.let { desc += " • $it" }
            track.description?.let { desc += "\n$it" }
            binding.trackDescription.apply {
                text = desc
                isVisible = desc.isNotEmpty()
            }

            applyChips(
                track.audioStreamables,
                binding.streamableAudios,
                binding.streamableAudiosGroup,
                item.audioIndex
            )

            applyChips(
                listOf(null, *track.videoStreamables.toTypedArray()),
                binding.streamableVideos,
                binding.streamableVideosGroup,
                item.videoIndex + 1
            )

            applyChips(
                listOf(null, *track.subtitleStreamables.toTypedArray()),
                binding.streamableSubtitles,
                binding.streamableSubtitleGroup,
                item.subtitleIndex + 1
            )

            val extension =
                playerViewModel.extensionListFlow.getExtension(item.clientId) ?: return@observe
            val adapter =
                MediaContainerAdapter(this, "track_details", extension.info, mediaClickListener)
            mediaAdapter = adapter
            binding.relatedRecyclerView.adapter = adapter.withLoaders()
            viewModel.load(item.clientId, track)
        }

        observe(viewModel.itemsFlow) {
            mediaAdapter?.submit(it)
        }

        observe(playerViewModel.browser) { player ->
            player ?: return@observe
            applyTracks(player.currentTracks)
            player.addListener(object : Player.Listener {
                @OptIn(UnstableApi::class)
                override fun onTracksChanged(tracks: Tracks) = applyTracks(tracks)
            })
        }
    }

    private fun applyTracks(tracks: Tracks) {
        val audios = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val videos = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        val subtitles = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

        val audio = applyChipsGetSelected(audios,
            binding.trackAudios,
            binding.trackAudiosGroup,
            { it.toAudioDetails() })
        val video = applyChipsGetSelected(videos,
            binding.trackVideos,
            binding.trackVideosGroup,
            { it.toVideoDetails() })

        val subtitle = applyChipsGetSelected(subtitles,
            binding.trackSubtitles,
            binding.trackSubtitlesGroup,
            { it.toSubtitleDetails() })

        val list = listOfNotNull(audio?.toAudioDetails(),
            video?.toVideoDetails(),
            subtitle?.toSubtitleDetails()?.takeIf { it != "Unknown" })
        println("list : $list")
        binding.streamableInfo.isVisible = list.isNotEmpty()
        binding.streamableInfo.text = list.joinToString("\n")
    }

    @OptIn(UnstableApi::class)
    private fun Format.getBitrate() =
        (bitrate / 1000).takeIf { it > 0 }?.let { " • ${it}kbps" } ?: ""

    private fun Format.getFrameRate() =
        frameRate.toInt().takeIf { it > 0 }?.let { " • $it fps" } ?: ""

    private fun Format.toVideoDetails() = "${height}p${getFrameRate()}${getBitrate()}"

    private fun Format.getMimeType() = when (val mime = sampleMimeType?.replace("audio/", "")) {
        "mp4a-latm" -> "AAC"
        else -> mime?.uppercase()
    }

    private fun Format.getHertz() =
        sampleRate.takeIf { it > 0 }?.let { " • $it Hz" } ?: ""

    @OptIn(UnstableApi::class)
    private fun Format.toAudioDetails() =
        "${getMimeType()}${getHertz()}• ${channelCount}ch${getBitrate()}"


    private fun Format.toSubtitleDetails() = label ?: language ?: "Unknown"

    @OptIn(UnstableApi::class)
    private fun applyChipsGetSelected(
        groups: List<Tracks.Group>,
        textView: TextView,
        chipGroup: ChipGroup,
        text: (Format) -> String = {
            it.toString()
        },
        onClick: Chip.(Pair<Tracks.Group, Int>?) -> Unit = {
            println("group : ${it!!.first.getTrackFormat(it.second)}")
        }
    ): Format? {
        var selected: Pair<Tracks.Group, Int>? = null
        val trackGroups = groups.map { trackGroup ->
            (0 until trackGroup.length).map { i ->
                val pair = Pair(trackGroup, i)
                val isSelected = trackGroup.isTrackSelected(i)
                if (isSelected) selected = pair
                pair
            }
        }.flatten()
        val select = trackGroups.indexOf(selected).takeIf { it != -1 }
        applyChips(
            trackGroups, textView, chipGroup, select, {
                val format = it!!.first.getTrackFormat(it.second)
                text(format)
            }, onClick
        )
        return selected?.run { first.getTrackFormat(second) }
    }

    private fun applyChips(
        streamables: List<Streamable?>,
        textView: TextView,
        chipGroup: ChipGroup,
        selected: Int?,
        onClick: Chip.(Streamable?) -> Unit = { println("clicked : $it") }
    ) {
        applyChips(
            streamables,
            textView,
            chipGroup,
            selected,
            { it?.title ?: it?.quality?.toString() ?: getString(R.string.off) },
            onClick
        )
    }

    private fun <T> applyChips(
        list: List<T?>,
        textView: TextView,
        chipGroup: ChipGroup,
        selected: Int?,
        text: (T?) -> String,
        onClick: Chip.(T?) -> Unit
    ) {
        val visible = list.size > 1
        println("${textView.text} $visible : ${list.size}")
        textView.isVisible = visible
        chipGroup.apply {
            isVisible = visible
            removeAllViews()
            fun makeChip(text: String, onClick: Chip.(T?) -> Unit): Chip {
                val chip = Chip(context)
                chip.text = text
                chip.isCheckable = true
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) onClick(chip, null)
                }
                addView(chip)
                return chip
            }

            list.forEachIndexed { index, t ->
                val chip = makeChip(text(t)) {
                    onClick(this, t)
                }
                if (index == selected) check(chip.id)
            }
        }
    }

    @HiltViewModel
    class TrackDetailsViewModel @Inject constructor(
        throwableFlow: MutableSharedFlow<Throwable>,
        val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    ) : CatchingViewModel(throwableFlow) {

        private var previous: Track? = null
        val itemsFlow = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)

        fun load(clientId: String, track: Track) {
            if (previous?.id == track.id) return
            previous = track
            itemsFlow.value = null
            val extension = extensionListFlow.getExtension(clientId) ?: return
            val client = extension.client
            if (client !is TrackClient) return
            viewModelScope.launch {
                tryWith(extension.info) {
                    client.getMediaItems(track).toFlow().collectTo(itemsFlow)
                }
            }
        }
    }
}


