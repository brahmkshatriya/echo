package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.setupPlayerInfoBehavior

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

        val adapter = PlayerTrackAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.setPageTransformer(
            ParallaxPageTransformer(R.id.expandedTrackCoverContainer)
        )
        val changeCallback = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (viewModel.currentIndex.value != position)
                    emit(viewModel.audioIndexFlow) { position }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(changeCallback)

        binding.viewPager.getChildAt(0).run {
            this as RecyclerView
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        observe(viewModel.listChangeFlow) {
            adapter.submitList(it)
            if (it.isEmpty()) {
                emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
                emit(uiViewModel.changePlayerState) { STATE_HIDDEN }
            } else {
                if (uiViewModel.playerSheetState.value == STATE_HIDDEN) {
                    emit(uiViewModel.changePlayerState) { STATE_COLLAPSED }
                    emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
                }
            }
        }

        observe(uiViewModel.playerSheetState) {
            if (it == STATE_HIDDEN) viewModel.clearQueue()
        }

        observe(viewModel.currentIndex) {
            it ?: return@observe
            binding.viewPager.currentItem = it
        }
    }

}

