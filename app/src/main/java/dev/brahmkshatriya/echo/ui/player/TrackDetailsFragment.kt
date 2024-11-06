package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.app.Application
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentTrackDetailsBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackInfoBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.run
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.ui.adapter.ShelfClickListener
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
        val shelfClickListener = ShelfClickListener(requireActivity().supportFragmentManager) {
            uiViewModel.collapsePlayer()
        }

        val infoAdapter = InfoAdapter(playerViewModel)
        var mediaAdapter: ShelfAdapter? = null
        observe(playerViewModel.currentFlow) { current ->
            val (_, item) = current ?: return@observe
            item.takeIf { it.isLoaded } ?: return@observe
            val track = item.track
            infoAdapter.applyCurrent(item)

            if (viewModel.previous?.id == track.id) return@observe

            val extension =
                playerViewModel.extensionListFlow.getExtension(item.clientId) ?: return@observe
            val adapter =
                ShelfAdapter(this, "track_details", extension, shelfClickListener)
            mediaAdapter = adapter
            binding.root.adapter = ConcatAdapter(infoAdapter, adapter.withLoaders())
            viewModel.load(item.clientId, track)
        }

        observe(viewModel.itemsFlow) {
            mediaAdapter?.submit(it)
        }

        observe(playerViewModel.currentSources) {
            infoAdapter.applySources(it)
        }

        observe(playerViewModel.browser) { player ->
            player ?: return@observe
            infoAdapter.applyTracks(player, player.currentTracks)
            player.addListener(object : Player.Listener {
                @OptIn(UnstableApi::class)
                override fun onTracksChanged(tracks: Tracks) {
                    infoAdapter.applyTracks(player, tracks)
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    class InfoAdapter(
        private val playerViewModel: PlayerViewModel
    ) : RecyclerView.Adapter<InfoAdapter.ViewHolder>() {

        private var sources: Streamable.Media.Sources? = null
        private var item: MediaItem? = null
        private var tracks: Tracks? = null
        private var player: Player? = null

        inner class ViewHolder(val binding: ItemTrackInfoBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemTrackInfoBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount() = 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val binding = holder.binding
            item?.let { binding.applyCurrent(it) }
            player?.let { binding.applyTracks(it, tracks ?: Tracks.EMPTY) }
            sources?.let { binding.applySources(it) }
        }

        fun applyCurrent(item: MediaItem) {
            this.item = item
            notifyDataSetChanged()
        }

        fun applyTracks(player: Player, tracks: Tracks) {
            this.player = player
            this.tracks = tracks
            notifyDataSetChanged()
        }

        fun applySources(sources: Streamable.Media.Sources?) {
            this.sources = sources
            notifyDataSetChanged()
        }

        private fun ItemTrackInfoBinding.applyCurrent(item: MediaItem) {
            val context = root.context
            val track = item.track
            var desc = ""
            track.plays?.let { desc += context.getString(R.string.track_total_plays, it) }
            track.releaseDate?.let { desc += " • $it" }
            track.description?.let { desc += "\n$it" }
            trackDescription.apply {
                text = desc
                isVisible = desc.isNotEmpty()
            }

            applyChips(
                track.sources,
                streamableSources,
                streamableSourcesGroup,
                item.sourcesIndex
            ) {
                it ?: return@applyChips
                val index = track.sources.indexOf(it)
                val newItem = MediaItemUtils.buildSources(item, index)
                playerViewModel.withBrowser { player ->
                    player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                }
            }

            applyChips(
                listOf(null, *track.backgrounds.toTypedArray()),
                streamableBackgrounds,
                streamableBackgroundGroup,
                item.backgroundIndex + 1
            ) {
                val index = track.backgrounds.indexOf(it)
                val newItem = MediaItemUtils.buildBackground(item, index)
                playerViewModel.withBrowser { player ->
                    player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                }
            }

            applyChips(
                listOf(null, *track.subtitles.toTypedArray()),
                streamableSubtitles,
                streamableSubtitleGroup,
                item.subtitleIndex + 1
            ) {
                val index = track.subtitles.indexOf(it)
                val newItem = MediaItemUtils.buildSubtitle(item, index)
                playerViewModel.withBrowser { player ->
                    player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                }
            }
        }

        private fun ItemTrackInfoBinding.applyTracks(player: Player, tracks: Tracks) {
            val audios = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            val videos = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
            val subtitles = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

            val param: Chip.(Pair<Tracks.Group, Int>?) -> Unit = {
                val trackGroup = it!!.first.mediaTrackGroup
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverride(trackGroup)
                    .addOverride(TrackSelectionOverride(trackGroup, it.second))
                    .build()
            }

            val audio = applyChipsGetSelected(
                audios,
                trackAudios,
                trackAudiosGroup,
                { it.toAudioDetails() },
                param
            )

            val video = applyChipsGetSelected(
                videos,
                trackVideos,
                trackVideosGroup,
                { it.toVideoDetails() },
                param
            )

            val subtitle = applyChipsGetSelected(
                subtitles, trackSubtitles,
                trackSubtitlesGroup,
                { it.toSubtitleDetails() },
                param
            )

            val list = listOfNotNull(audio?.toAudioDetails(),
                video?.toVideoDetails(),
                subtitle?.toSubtitleDetails()?.takeIf { it != "Unknown" })

            streamableInfo.isVisible = list.isNotEmpty()
            streamableInfo.text = list.joinToString("\n")
        }

        @OptIn(UnstableApi::class)
        private fun Format.getBitrate() =
            (bitrate / 1024).takeIf { it > 0 }?.let { " • $it kbps" } ?: ""

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
            "${getMimeType()}${getHertz()} • ${channelCount}ch${getBitrate()}"


        private fun Format.toSubtitleDetails() = label ?: language ?: "Unknown"

        @OptIn(UnstableApi::class)
        private fun applyChipsGetSelected(
            groups: List<Tracks.Group>,
            textView: TextView,
            chipGroup: ChipGroup,
            text: (Format) -> String,
            onClick: Chip.(Pair<Tracks.Group, Int>?) -> Unit
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
                    val format = it.first.getTrackFormat(it.second)
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
            onClick: Chip.(Streamable?) -> Unit
        ) {
            val context = chipGroup.context
            applyChips(
                streamables,
                textView,
                chipGroup,
                selected,
                {
                    it?.let {
                        it.title ?: when (it.type) {
                            Streamable.MediaType.Subtitle -> context.getString(R.string.unknown)
                            else -> context.getString(R.string.quality_number, it.quality)
                        }
                    } ?: context.getString(R.string.off)
                },
                onClick
            )
        }

        private fun ItemTrackInfoBinding.applySources(sources: Streamable.Media.Sources?) {
            val list = if(sources != null && !sources.merged) sources.sources else listOf()
            val context = root.context
            applyChips(
                list,
                streamableSource,
                streamableSourceGroup,
                item?.sourceIndex,
                { it.title ?: context.getString(R.string.quality_number, it.quality) },
                {
                    val index = list.indexOf(it)
                    val newItem = MediaItemUtils.buildSource(item!!, index)
                    playerViewModel.withBrowser { player ->
                        player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                    }
                }
            )
        }

        private fun <T> applyChips(
            list: List<T>,
            textView: TextView,
            chipGroup: ChipGroup,
            selected: Int?,
            text: (T) -> String,
            onClick: Chip.(T) -> Unit
        ) {
            val visible = list.size > 1
            textView.isVisible = visible
            chipGroup.apply {
                isVisible = visible
                removeAllViews()
                list.forEachIndexed { index, t ->
                    val chip = Chip(context)
                    chip.text = text(t)
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


    @HiltViewModel
    class TrackDetailsViewModel @Inject constructor(
        val app: Application,
        val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
        throwableFlow: MutableSharedFlow<Throwable>,
    ) : CatchingViewModel(throwableFlow) {

        var previous: Track? = null
        val itemsFlow = MutableStateFlow<PagingData<Shelf>?>(null)

        fun load(clientId: String, track: Track) {
            previous = track
            itemsFlow.value = null

            val extension = extensionListFlow.getExtension(clientId) ?: return
            val client = extension.instance.value.getOrNull()
            val album = track.album
            val artists = track.artists

            viewModelScope.launch {
                val pagedData = PagedData.Concat(
                    if (client is AlbumClient && album != null) PagedData.Single {
                        listOf(
                            client.loadAlbum(album).toMediaItem().toShelf()
                        )
                    } else PagedData.empty(),
                    if (artists.isNotEmpty()) PagedData.Single {
                        listOf(
                            Shelf.Lists.Items(
                                app.getString(R.string.artists),
                                if (client is ArtistClient) artists.map {
                                    val artist = client.loadArtist(it)
                                    artist.toMediaItem()
                                } else artists.map { it.toMediaItem() }
                            )
                        )
                    } else PagedData.empty(),
                    if (client is TrackClient) extension.run(throwableFlow) {
                        client.getShelves(track)
                    } ?: PagedData.empty()
                    else PagedData.empty()
                )
                pagedData.toFlow().collectTo(itemsFlow)
            }
        }
    }
}