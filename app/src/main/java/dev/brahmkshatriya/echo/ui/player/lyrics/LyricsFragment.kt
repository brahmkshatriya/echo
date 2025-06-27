package dev.brahmkshatriya.echo.ui.player.lyrics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.databinding.FragmentPlayerLyricsBinding
import dev.brahmkshatriya.echo.databinding.ItemLyricsItemBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel


class LyricsFragment : Fragment() {

    private var binding by autoCleared<FragmentPlayerLyricsBinding>()
    private val viewModel by activityViewModel<LyricsViewModel>()
    private val playerVM by activityViewModel<PlayerViewModel>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerLyricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var currentLyricsPos = -1
    private var currentLyrics: Lyrics.Lyric? = null
    private val lyricAdapter by lazy {
        LyricAdapter(uiViewModel) { adapter, lyric ->
            if (adapter.itemCount <= 1) return@LyricAdapter
            currentLyricsPos = -1
            playerVM.seekTo(lyric.startTime)
            updateLyrics(lyric.startTime)
        }
    }

    private var shouldAutoScroll = true
    val layoutManager by lazy {
        binding.lyricsRecyclerView.layoutManager as LinearLayoutManager
    }

    private fun updateLyrics(current: Long) {
        val lyrics = currentLyrics as? Lyrics.Timed ?: return
        val currentTime = lyrics.list.getOrNull(currentLyricsPos)?.endTime ?: -1
        if (currentTime < current || current <= 0) {
            val currentIndex = lyrics.list.indexOfLast { lyric ->
                lyric.startTime <= current
            }
            lyricAdapter.updateCurrent(currentIndex)
            if (!shouldAutoScroll) return
            binding.appBarLayout.setExpanded(false)
            slideDown()
            if (currentIndex < 0) return
            val smoothScroller = CenterSmoothScroller(requireContext())
            smoothScroller.targetPosition = currentIndex
            layoutManager.startSmoothScroll(smoothScroller)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, false)
        FastScrollerHelper.applyTo(binding.lyricsRecyclerView)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, _ -> CONSUMED }
        observe(uiViewModel.moreSheetState) {
            binding.root.keepScreenOn = it == BottomSheetBehavior.STATE_EXPANDED
        }
        binding.searchBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_lyrics -> ExtensionsListBottomSheet.newInstance(ExtensionType.LYRICS)
                    .show(parentFragmentManager, null)
            }
            true
        }
        val menu = binding.searchBar.menu
        val extMenu = binding.searchBar.findViewById<View>(R.id.menu_lyrics)
        extMenu.setOnLongClickListener {
            val ext = viewModel.currentSelectionFlow.value ?: return@setOnLongClickListener false
            val all = viewModel.extensionsFlow.value
            val index = all.indexOf(ext)
            val nextIndex = (index + 1) % all.size
            if (nextIndex == index) return@setOnLongClickListener false
            viewModel.selectExtension(nextIndex)
            true
        }
        val lyricsItemAdapter = LyricsItemAdapter { lyrics ->
            viewModel.onLyricsSelected(lyrics)
            binding.searchView.hide()
        }
        binding.searchRecyclerView.adapter = lyricsItemAdapter.withLoaders(this)
        observe(viewModel.currentSelectionFlow) { current ->
            binding.searchBar.hint = current?.name
            current?.metadata?.icon.loadAsCircle(extMenu, R.drawable.ic_extension_48dp) {
                menu.findItem(R.id.menu_lyrics).icon = it
            }
            val isSearchable = current?.isClient<LyricsSearchClient>() ?: false
            binding.searchBar.setNavigationIcon(
                when (isSearchable) {
                    true -> R.drawable.ic_search_outline
                    false -> R.drawable.ic_queue_music
                }
            )
            binding.searchView.editText.isEnabled = isSearchable
            binding.searchView.hint = if (isSearchable)
                getString(R.string.search_x, current?.name)
            else current?.name
        }

        binding.searchView.editText.setOnEditorActionListener { v, _, _ ->
            viewModel.search(v.text.toString().takeIf { it.isNotBlank() })
            true
        }


        binding.searchRecyclerView.adapter = lyricsItemAdapter
        observe(viewModel.searchResults) {
            lyricsItemAdapter.submitData(it ?: PagingData.empty())
        }

        var job: Job? = null
        binding.lyricsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) return
                shouldAutoScroll = false
                job?.cancel()
                job = lifecycleScope.launch {
                    delay(3500)
                    shouldAutoScroll = true
                }
            }
        })

        observe(uiViewModel.playerColors) {
            lyricAdapter.updateColors()
            val colors = it ?: requireContext().defaultPlayerColors()
            binding.noLyrics.setTextColor(colors.onBackground)
            binding.loading.apply {
                progress.setIndicatorColor(colors.accent)
                textView.setTextColor(colors.onBackground)
            }
        }

        binding.lyricsRecyclerView.adapter = lyricAdapter
        binding.lyricsRecyclerView.itemAnimator = null
        observe(viewModel.lyricsState) {
            val lyricsItem = (it as? LyricsViewModel.State.Loaded)?.lyrics
            binding.lyricsItem.bind(lyricsItem)
            currentLyricsPos = -1
            currentLyrics = lyricsItem?.lyrics
            val list = when (val lyrics = currentLyrics) {
                is Lyrics.Simple -> listOf(Lyrics.Item(lyrics.text, 0, 0))
                is Lyrics.Timed -> lyrics.list
                null -> emptyList()
            }
            lyricAdapter.submitList(list)
            binding.noLyrics.isVisible = when (it) {
                LyricsViewModel.State.Empty -> true
                is LyricsViewModel.State.Loaded -> list.isEmpty()
                else -> false
            }
            binding.loading.root.isVisible = it == LyricsViewModel.State.Loading
        }

        observe(playerVM.progress) { updateLyrics(it.first) }
    }

    fun ItemLyricsItemBinding.bind(lyrics: Lyrics?) = root.run {
        if (lyrics == null) {
            isVisible = false
            return
        }
        isVisible = true
        setTitle(lyrics.title)
        setSubtitle(lyrics.subtitle)
    }

    class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int
        ): Int {
            val midPoint = boxEnd / 2
            val targetMidPoint = ((viewEnd - viewStart) / 2) + viewStart
            return midPoint - targetMidPoint
        }

        override fun getVerticalSnapPreference() = SNAP_TO_START
        override fun calculateTimeForDeceleration(dx: Int) = 650
    }

    @SuppressLint("WrongConstant")
    private fun slideDown() {
        val params = binding.lyricsItem.root.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as HideViewOnScrollBehavior
        behavior.setViewEdge(1)
        behavior.slideOut(binding.lyricsItem.root)
    }
}