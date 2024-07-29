package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.FragmentPlayerBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.item.ItemBottomSheet
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isLandscape
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.setupPlayerInfoBehavior
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

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

        setupPlayerInfoBehavior(uiViewModel, binding.playerInfoContainer)

        val adapter = PlayerTrackAdapter(this, object : PlayerTrackAdapter.Listener {
            override fun onMoreClicked(clientId: String?, item: EchoMediaItem, loaded: Boolean) {
                if (clientId == null) {
                    createSnack(requireContext().noClient())
                    return
                }
                ItemBottomSheet.newInstance(clientId, item, loaded, true)
                    .show(parentFragmentManager, null)
            }

            override fun onItemClicked(clientId: String?, item: EchoMediaItem) {
                if (clientId == null) {
                    createSnack(requireContext().noClient())
                    return
                }
                requireActivity().openFragment(ItemFragment.newInstance(clientId, item))
                uiViewModel.collapsePlayer()
            }

        })
        binding.viewPager.adapter = adapter
        binding.viewPager.setPageTransformer(
            ParallaxPageTransformer(R.id.expandedTrackCoverContainer)
        )

        binding.viewPager.registerOnUserPageChangeCallback { position, userInitiated ->
            if (viewModel.currentFlow.value?.index != position && userInitiated)
                lifecycleScope.launch { viewModel.play(position) }
        }

        binding.viewPager.getChildAt(0).run {
            this as RecyclerView
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val combined = viewModel.run { currentFlow.combine(listUpdateFlow) { it, _ -> it } }
        observe(combined) {
            val list = viewModel.list
            if (list.isEmpty()) {
                emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
                emit(uiViewModel.changePlayerState) { STATE_HIDDEN }
            } else {
                if (uiViewModel.playerSheetState.value == STATE_HIDDEN) {
                    emit(uiViewModel.changePlayerState) { STATE_COLLAPSED }
                    emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
                }
            }
            adapter.submitList(list)
            val index = it?.index ?: -1
            val smooth = abs(index - binding.viewPager.currentItem) <= 1
            binding.viewPager.setCurrentItem(index, smooth)
        }

        observe(uiViewModel.playerSheetState) {
            if (it == STATE_HIDDEN) viewModel.clearQueue()
        }

        observe(uiViewModel.playerSheetOffset) {
            val offset = max(0f, it)
            binding.playerOutline.alpha = 1 - offset
        }

        observe(uiViewModel.infoSheetState) {
            binding.viewPager.isUserInputEnabled =
                requireContext().isLandscape() || it == STATE_COLLAPSED
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

