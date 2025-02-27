package dev.brahmkshatriya.echo.ui.player

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerBinding
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyHorizontalInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.isFinalState
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.setupPlayerMoreBehavior
import dev.brahmkshatriya.echo.utils.ContextUtils.emit
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.animateVisibility
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoClearedNullable
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.hideSystemUi
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isLandscape
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isRTL
import dev.brahmkshatriya.echo.utils.ui.ViewPager2Utils.registerOnUserPageChangeCallback
import dev.brahmkshatriya.echo.utils.ui.ViewPager2Utils.supportBottomSheetBehavior
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlayerFragment : Fragment() {
    private var binding by autoClearedNullable<FragmentPlayerBinding>()
    private val viewModel by activityViewModel<PlayerViewModel>()
    private val uiViewModel by activityViewModel<UiViewModel>()
    private val adapter by lazy { PlayerTrackAdapter(uiViewModel) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        binding.viewPager.supportBottomSheetBehavior()
        setupPlayerMoreBehavior(uiViewModel, binding.playerMoreContainer)
        configureOutline(binding.root)
        configureCollapsing(binding)
        configurePlayerState()
    }

    private val collapseHeight by lazy {
        resources.getDimension(R.dimen.collapsed_cover_size).toInt()
    }

    private fun configureOutline(view: View) {
        val padding = 8.dpToPx(requireContext())
        var currHeight = collapseHeight
        var currRound = padding.toFloat()
        var currRight = 0
        var currLeft = 0
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    currLeft, 0, currRight, currHeight, currRound
                )
            }
        }
        view.clipToOutline = true

        var leftPadding = 0
        var rightPadding = 0

        val maxElevation = 4.dpToPx(requireContext()).toFloat()
        fun updateOutline() {
            val offset = max(0f, uiViewModel.playerSheetOffset.value)
            val inv = 1 - offset
            view.elevation = maxElevation * inv
            currHeight = collapseHeight + ((view.height - collapseHeight) * offset).toInt()
            currLeft = (leftPadding * inv).toInt()
            currRight = view.width - (rightPadding * inv).toInt()
            currRound = max(padding * inv, padding * uiViewModel.playerBackProgress.value * 2)
            view.invalidateOutline()
        }
        observe(uiViewModel.combined) {
            leftPadding = (if (view.context.isRTL()) it.end else it.start) + padding
            rightPadding = (if (view.context.isRTL()) it.start else it.end) + padding
            updateOutline()
        }
        observe(uiViewModel.playerBackProgress) { updateOutline() }
        observe(uiViewModel.playerSheetOffset) { updateOutline() }
        view.doOnLayout { updateOutline() }
    }

    private fun configureCollapsing(binding: FragmentPlayerBinding) {
        binding.playerCollapsedContainer.clipToOutline = true

        val collapsedTopPadding = 8.dpToPx(requireContext())
        var currRound = collapsedTopPadding.toFloat()
        var currTop = 0
        var currBottom = collapseHeight
        var currRight = 0
        var currLeft = 0

        val view = binding.viewPager
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    currLeft, currTop, currRight, currBottom, currRound
                )
            }
        }
        view.clipToOutline = true

        val extraEndPadding = 108.dpToPx(requireContext())
        var leftPadding = 0
        var rightPadding = 0
        val isLandscape = requireContext().isLandscape()
        fun updateCollapsed() {
            val (collapsedY, offset, collapsedOffset) = uiViewModel.run {
                if (playerSheetState.value == STATE_EXPANDED) {
                    val offset = moreSheetOffset.value
                    Triple(systemInsets.value.top, offset, if (isLandscape) 0f else offset)
                } else {
                    val offset = 1 - max(0f, playerSheetOffset.value)
                    Triple(-collapsedTopPadding, offset, offset)
                }
            }
            val collapsedInv = 1 - collapsedOffset
            binding.playerCollapsedContainer.run {
                translationY = collapsedY - collapseHeight * collapsedInv * 2
                alpha = collapsedOffset * 2
                translationZ = -1f * collapsedInv
            }
            binding.bgCollapsed.run {
                translationY = collapsedY - collapseHeight * collapsedInv * 2
                alpha = collapsedOffset * 2
            }
            val alphaInv = 1 - min(1f, offset * 3)
            binding.expandedToolbar.run {
                translationY = collapseHeight * offset * 2
                alpha = alphaInv
                isVisible = offset < 1
                translationZ = -1f * offset
            }
            binding.playerControls.root.run {
                translationY = collapseHeight * offset * 2
                alpha = alphaInv
                isVisible = offset < 1
            }
            currTop = uiViewModel.run {
                val top = if (playerSheetState.value != STATE_EXPANDED) 0
                else collapsedTopPadding + systemInsets.value.top
                (top * max(0f, (collapsedOffset - 0.75f) * 4)).toInt()
            }
            val bot = currTop + collapseHeight
            currBottom = bot + ((view.height - bot) * collapsedInv).toInt()
            currLeft = (leftPadding * collapsedOffset).toInt()
            currRight = view.width - (rightPadding * collapsedOffset).toInt()
            currRound = collapsedTopPadding * collapsedOffset
            view.invalidateOutline()
        }

        view.doOnLayout { updateCollapsed() }
        observe(uiViewModel.systemInsets) {
            binding.constraintLayout.applyInsets(it, 64, 0)
            binding.expandedToolbar.applyInsets(it)
            val insets = uiViewModel.run {
                if (playerSheetState.value == STATE_EXPANDED) systemInsets.value
                else getCombined()
            }
            binding.playerCollapsedContainer.applyHorizontalInsets(insets)
            val left = if (requireContext().isRTL()) it.end + extraEndPadding else it.start
            leftPadding = collapsedTopPadding + left
            val right = if (requireContext().isRTL()) it.start else it.end + extraEndPadding
            rightPadding = collapsedTopPadding + right
            updateCollapsed()
            adapter.systemInsetsUpdated()
        }

        observe(uiViewModel.moreSheetOffset) {
            updateCollapsed()
            adapter.moreOffsetUpdated()
        }
        observe(uiViewModel.playerSheetOffset) {
            updateCollapsed()
            adapter.playerOffsetUpdated()

            viewModel.browser.value?.volume = 1 + min(0f, it)
            if (it < 1)
                requireActivity().hideSystemUi(false)
            else if (uiViewModel.playerBgVisibleState.value)
                requireActivity().hideSystemUi(true)
        }

        observe(uiViewModel.playerSheetState) {
            updateCollapsed()
            if (isFinalState(it)) adapter.playerSheetStateUpdated()
            if (it == STATE_HIDDEN) viewModel.clearQueue()
            else if (it == STATE_COLLAPSED) emit(uiViewModel.playerBgVisibleState, false)
        }

        binding.playerControls.root.doOnLayout {
            uiViewModel.playerControlsHeight.value = it.height
            adapter.playerControlsHeightUpdated()
        }
        observe(uiViewModel.playerBgVisibleState) {
            binding.fgContainer.animateVisibility(!it)
            requireActivity().hideSystemUi(it)
        }
    }

    private fun configurePlayerState() {
        val viewPager = binding!!.viewPager
        viewPager.adapter = adapter
        var last: Job? = null
        viewPager.registerOnUserPageChangeCallback { pos, isUser ->
            if (viewModel.playerState.current.value?.index != pos && isUser) {
                last?.cancel()
                last = lifecycleScope.launch {
                    delay(800)
                    viewModel.seek(pos)
                }
            }
        }
        fun submit() {
            adapter.submitList(viewModel.queue) {
                val index = viewModel.playerState.current.value?.index ?: -1
                val current = (binding?.viewPager?.currentItem ?: -1).takeIf { it != -1 }
                    ?: return@submitList
                val smooth = abs(index - current) <= 1
                binding?.viewPager?.setCurrentItem(index, smooth)
            }
        }
        observe(viewModel.playerState.current) {
            uiViewModel.run {
                changePlayerState(
                    if (it == null) STATE_HIDDEN
                    else if (playerSheetState.value != STATE_EXPANDED) STATE_COLLAPSED
                    else STATE_EXPANDED
                )
            }
            submit()
        }
        observe(viewModel.queueFlow) { submit() }
    }

    companion object {
        const val SHOW_BACKGROUND = "show_background"
        const val DYNAMIC_PLAYER = "dynamic_player"
    }
}