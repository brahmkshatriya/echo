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
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerInfoBinding
import dev.brahmkshatriya.echo.ui.player.info.TrackDetailsFragment
import dev.brahmkshatriya.echo.ui.player.lyrics.LyricsFragment
import dev.brahmkshatriya.echo.ui.player.upnext.QueueFragment
import dev.brahmkshatriya.echo.utils.ui.SlideInPageTransformer
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

class PlayerInfoFragment : Fragment() {
    var binding by autoCleared<FragmentPlayerInfoBinding>()
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

        fun applyChange() {
            val offset = uiViewModel.infoSheetOffset.value
            binding.buttonToggleGroupContainer.run {
                translationY = offset * uiViewModel.systemInsets.value.top
                binding.viewCard.translationY = (1 - offset) * uiViewModel.systemInsets.value.bottom
            }
        }

        observe(uiViewModel.systemInsets) {
            binding.viewCard.updateLayoutParams<MarginLayoutParams> { topMargin = it.top }
            applyChange()
        }

        observe(uiViewModel.infoSheetOffset) {
            binding.buttonToggleGroupBg.alpha = it
            applyChange()
        }

        observe(uiViewModel.infoSheetState) {
            binding.buttonToggleGroupFg.run {
                if (it == STATE_COLLAPSED) clearChecked()
                else if (it == STATE_EXPANDED) check(idsFg[binding.viewPager.currentItem])
            }
        }

        binding.viewPager.getChildAt(0).run {
            this as RecyclerView
            overScrollMode = View.OVER_SCROLL_NEVER
            observe(uiViewModel.infoSheetState) {
                if (it == STATE_DRAGGING || it == STATE_SETTLING) return@observe
                isNestedScrollingEnabled = it != STATE_COLLAPSED
            }
        }

        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = PlayerInfoAdapter(this)
        binding.viewPager.setPageTransformer(SlideInPageTransformer())

        binding.buttonToggleGroupFg.addOnButtonCheckedListener { group, checkedId, isChecked ->
            uiViewModel.changeInfoState(
                if (group.checkedButtonId == -1) STATE_COLLAPSED else STATE_EXPANDED
            )
            if (!isChecked) return@addOnButtonCheckedListener
            val index = idsFg.indexOf(checkedId)
            binding.buttonToggleGroupBg.check(idsBg[index])
            binding.viewPager.setCurrentItem(index, false)
        }
    }

    private val idsFg = listOf(R.id.upNextFg, R.id.lyricsFg, R.id.infoFg)
    private val idsBg = listOf(R.id.upNextBg, R.id.lyricsBg, R.id.infoBg)

    class PlayerInfoAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int) = when (position) {
            0 -> QueueFragment()
            1 -> LyricsFragment()
            2 -> TrackDetailsFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }

    }
}