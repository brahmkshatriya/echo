package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.FragmentPlayerInfoBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

class PlayerInfoFragment : Fragment() {
    var binding by autoCleared<FragmentPlayerInfoBinding>()
    private val viewModel by activityViewModels<PlayerViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        observe(uiViewModel.systemInsets){
            binding.viewCard.updateLayoutParams<MarginLayoutParams> {
                topMargin = it.top
            }
        }

        observe(uiViewModel.infoSheetOffset) {
            binding.buttonToggleGroup.translationY = it * uiViewModel.systemInsets.value.top
            binding.viewCard.alpha = it
        }

        binding.viewPager.getChildAt(0).run {
            this as RecyclerView
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }
}