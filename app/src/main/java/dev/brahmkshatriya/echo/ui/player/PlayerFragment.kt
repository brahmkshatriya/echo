package dev.brahmkshatriya.echo.ui.player

import android.app.Activity
import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.animateVisibility
import dev.brahmkshatriya.echo.utils.ui.dpToPx
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isLandscape
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isRTL
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.setupPlayerInfoBehavior
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlayerFragment : Fragment() {
    private var binding by autoCleared<FragmentPlayerBinding>()
    private val viewModel by activityViewModels<PlayerViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val maxRound = 8.dpToPx(requireContext())
        val padding = 8.dpToPx(requireContext())
        val height = resources.getDimension(R.dimen.collapsed_cover_size).toInt()

        var currHeight = height
        var currRound = maxRound.toFloat()
        var currRight = 0
        var currLeft = 0

        var leftPadding = 0
        var rightPadding = 0

        fun updateOutline() {
            val offset = max(0f, uiViewModel.playerSheetOffset.value)
            val inv = 1 - offset
            currHeight = height + ((view.height - height) * offset).toInt()
            currLeft = ((padding + leftPadding) * inv).toInt()
            currRight = view.width - ((padding + rightPadding) * inv).toInt()
            currRound = maxRound * inv
            binding.root.invalidateOutline()
        }
        observe(uiViewModel.combined) {
            leftPadding = if (view.context.isRTL()) it.end else it.start
            rightPadding = if (view.context.isRTL()) it.start else it.end
            updateOutline()
        }
        binding.root.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    currLeft, 0, currRight, currHeight, currRound
                )
            }
        }
        binding.root.clipToOutline = true
        binding.root.elevation = 4.dpToPx(requireContext()).toFloat()

        setupPlayerInfoBehavior(uiViewModel, binding.playerInfoContainer)
        val recycler = binding.viewPager.getChildAt(0) as RecyclerView
        recycler.run {
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        binding.viewPager.setPageTransformer(
            ParallaxPageTransformer(R.id.expandedTrackCoverContainer)
        )

        binding.viewPager.registerOnUserPageChangeCallback { position, userInitiated ->
            if (viewModel.currentFlow.value?.index != position && userInitiated)
                lifecycleScope.launch { viewModel.play(position) }
        }
        val adapter = PlayerTrackAdapter.newInstance(this@PlayerFragment, recycler)
        binding.viewPager.adapter = adapter

        fun update() {
            val list = viewModel.list
            adapter.submitList(list) {
                runCatching {
                    val index = viewModel.currentFlow.value?.index ?: -1
                    val smooth = abs(index - binding.viewPager.currentItem) <= 1
                    binding.viewPager.setCurrentItem(index, smooth)
                }.getOrElse {
                    it.printStackTrace()
                }
            }
            if (list.isEmpty()) {
                uiViewModel.changeInfoState(STATE_COLLAPSED)
                uiViewModel.changePlayerState(STATE_HIDDEN)
            } else {
                if (uiViewModel.playerSheetState.value == STATE_HIDDEN) {
                    uiViewModel.changePlayerState(STATE_COLLAPSED)
                    uiViewModel.changeInfoState(STATE_COLLAPSED)
                }
            }
        }

        observe(viewModel.listUpdateFlow) { update() }
        observe(viewModel.currentFlow) { update() }

        observe(uiViewModel.playerSheetState) {
            if (it == STATE_HIDDEN) viewModel.clearQueue()
            if (it == STATE_COLLAPSED) emit(uiViewModel.playerBgVisibleState) { false }
        }

        observe(uiViewModel.playerSheetOffset) {
            updateOutline()
            viewModel.browser.value?.volume = 1 + min(0f, it)
            val offset = max(0f, it)
            if (offset < 1)
                requireActivity().hideSystemUi(false)
            else if (uiViewModel.playerBgVisibleState.value)
                requireActivity().hideSystemUi(true)
        }

        observe(uiViewModel.infoSheetState) {
            binding.viewPager.isUserInputEnabled =
                requireContext().isLandscape() || it == STATE_COLLAPSED
        }

        observe(uiViewModel.playerBgVisibleState) {
            binding.playerInfoContainer.animateVisibility(!it)
            requireActivity().hideSystemUi(it)
        }
    }

    private fun Activity.hideSystemUi(hide: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hide) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun ViewPager2.registerOnUserPageChangeCallback(
        listener: (position: Int, userInitiated: Boolean) -> Unit
    ) {
        var previousState: Int = -1
        var userScrollChange = false
        registerOnPageChangeCallback(object : OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                listener(position, userScrollChange)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (previousState == ViewPager.SCROLL_STATE_DRAGGING &&
                    state == ViewPager.SCROLL_STATE_SETTLING
                ) {
                    userScrollChange = true
                } else if (previousState == ViewPager.SCROLL_STATE_SETTLING &&
                    state == ViewPager.SCROLL_STATE_IDLE
                ) {
                    userScrollChange = false
                }
                previousState = state
            }
        })
    }
}

