package dev.brahmkshatriya.echo.ui.player.lyrics

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.Timer
import java.util.TimerTask


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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, false)
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
        val extension = binding.searchBar.findViewById<View>(R.id.menu_lyrics)
        var lyricsItemAdapter: LyricsItemAdapter? = null
        observe(viewModel.currentSelectionFlow) { current ->
            binding.searchBar.hint = current?.name
            current?.metadata?.icon
                .loadAsCircle(extension, R.drawable.ic_extension_48dp) {
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
            lyricsItemAdapter = current?.let {
                LyricsItemAdapter(this, it) { lyrics ->
                    viewModel.onLyricsSelected(lyrics)
                    binding.searchView.hide()
                }
            }
            binding.searchRecyclerView.adapter = lyricsItemAdapter?.withLoaders()
        }

        binding.searchView.editText.setOnEditorActionListener { v, _, _ ->
            viewModel.search(v.text.toString().takeIf { it.isNotBlank() })
            true
        }


        binding.searchRecyclerView.adapter = lyricsItemAdapter
        observe(viewModel.searchResults) {
            lyricsItemAdapter?.submitData(it ?: PagingData.empty())
        }

        var currentItem: Lyrics.Item? = null
        var currentLyrics: Lyrics.Lyric? = null
        var lyricAdapter: LyricAdapter? = null

        val layoutManager = binding.lyricsRecyclerView.layoutManager as LinearLayoutManager
        var shouldAutoScroll = true
        var timer = Timer()
        binding.lyricsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0) {
                    shouldAutoScroll = false
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            shouldAutoScroll = true
                        }
                    }, 3000)
                }
            }
        })

        fun updateLyrics(current: Long) {
            val lyrics = currentLyrics as? Lyrics.Timed ?: return
            if ((currentItem?.endTime ?: 0) < current || current <= 0) {
                val list = lyrics.list.map { lyric ->
                    val isCurrent = lyric.startTime <= current
                    if (isCurrent) currentItem = lyric
                    isCurrent to lyric
                }
                lyricAdapter?.submitList(list)
                val currentIndex = list.indexOfLast { it.first }
                    .takeIf { it != -1 } ?: return

                if (shouldAutoScroll) {
                    val smoothScroller = CenterSmoothScroller(binding.lyricsRecyclerView)
                    smoothScroller.targetPosition = currentIndex
                    layoutManager.startSmoothScroll(smoothScroller)
                    binding.appBarLayout.setExpanded(false)
                    slideDown()
                }
            }
        }
        lyricAdapter = LyricAdapter(uiViewModel) { adapter, lyric ->
            if (adapter.itemCount <= 1) return@LyricAdapter
            currentItem = null
            playerVM.seekTo(lyric.startTime)
            updateLyrics(lyric.startTime)
        }

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
            currentItem = null
            currentLyrics = lyricsItem?.lyrics
            val list = when (val lyrics = currentLyrics) {
                is Lyrics.Simple -> listOf(true to Lyrics.Item(lyrics.text, 0, 0))
                is Lyrics.Timed -> lyrics.list.map { lyric -> false to lyric }
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

    class CenterSmoothScroller(private val recyclerView: RecyclerView) :
        LinearSmoothScroller(recyclerView.context) {

        override fun calculateDtToFit(
            viewStart: Int, viewEnd: Int,
            boxStart: Int, boxEnd: Int, snapPreference: Int
        ): Int {
            val midPoint = recyclerView.height / 2
            val targetMidPoint = ((viewEnd - viewStart) / 2) + viewStart
            return midPoint - targetMidPoint
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            return 100f / displayMetrics.densityDpi
        }

        override fun calculateTimeForScrolling(dx: Int): Int {
            val time = super.calculateTimeForScrolling(dx)
            return (time * 1.5f).toInt()
        }

        override fun calculateTimeForDeceleration(dx: Int): Int {
            return (super.calculateTimeForDeceleration(dx) * 1.5f).toInt()
        }
    }

    @SuppressLint("WrongConstant")
    private fun slideDown() {
        val params = binding.lyricsItem.root.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as HideViewOnScrollBehavior
        behavior.setViewEdge(1)
        behavior.slideOut(binding.lyricsItem.root)
    }
}