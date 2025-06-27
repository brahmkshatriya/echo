package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commitNow
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerMoreBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.info.InfoFragment
import dev.brahmkshatriya.echo.ui.player.lyrics.LyricsFragment
import dev.brahmkshatriya.echo.ui.player.upnext.QueueFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isLandscape
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class PlayerMoreFragment : Fragment() {

    private var binding by autoCleared<FragmentPlayerMoreBinding>()
    private val uiViewModel by activityViewModel<UiViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    private inline fun <reified F : Fragment> Fragment.addIfNull(): String {
        val tag = F::class.java.simpleName
        childFragmentManager.run {
            if (findFragmentByTag(tag) == null) commitNow {
                add<F>(R.id.player_more_fragment_container, tag)
            }
        }
        return tag
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var topInset = 0
        val topMargin = requireContext().run { if (isLandscape()) 0 else 72.dpToPx(this) }
        fun updateTranslateY() {
            val offset = uiViewModel.moreSheetOffset.value
            val inverted = 1 - offset
            binding.root.translationY = -topInset * inverted
            binding.playerMoreFragmentContainer.translationY =
                (1 - offset) * 2 * uiViewModel.systemInsets.value.bottom
        }

        observe(uiViewModel.systemInsets) {
            topInset = it.top + topMargin
            view.updatePaddingRelative(top = it.top)
            updateTranslateY()
        }

        observe(uiViewModel.moreSheetOffset) {
            updateTranslateY()
            binding.buttonToggleGroupBg.alpha = it
        }

        observe(uiViewModel.moreSheetState) {
            if (it == STATE_COLLAPSED) binding.buttonToggleGroup.clearChecked()
            else binding.buttonToggleGroup.check(uiViewModel.lastMoreTab)
        }

        observe(uiViewModel.playerColors) { colorsNullable ->
            val colors = colorsNullable ?: requireContext().defaultPlayerColors()
            binding.buttonToggleGroupBg.children.forEach {
                it as MaterialButton
                it.backgroundTintList = ColorStateList.valueOf(colors.background)
            }

            val textColorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    colors.background,
                    colors.onBackground
                )
            )
            val foregroundColorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    colors.onBackground,
                    android.graphics.Color.TRANSPARENT
                )
            )
            binding.buttonToggleGroup.children.forEach {
                it as MaterialButton
                it.setTextColor(textColorStateList)
                it.backgroundTintList = foregroundColorStateList
            }
        }

        showFragment()
        binding.buttonToggleGroup.addOnButtonCheckedListener { group, _, _ ->
            uiViewModel.run {
                val current = moreSheetState.value
                if (current != STATE_COLLAPSED && current != STATE_EXPANDED) return@run
                changeMoreState(
                    if (group.checkedButtonId != -1) STATE_EXPANDED else STATE_COLLAPSED
                )
            }
            showFragment()
        }

        @SuppressLint("ClickableViewAccessibility")
        val touchListener = View.OnTouchListener { v, _ ->
            uiViewModel.lastMoreTab = v.id
            false
        }
        binding.buttonToggleGroup.children.forEach { it.setOnTouchListener(touchListener) }
    }

    private fun showFragment() {
        val checkedId = binding.buttonToggleGroup.checkedButtonId
        val toShow = when (checkedId) {
            R.id.queue -> addIfNull<QueueFragment>()
            R.id.lyrics -> addIfNull<LyricsFragment>()
            R.id.info -> addIfNull<InfoFragment>()
            else -> null
        }
        childFragmentManager.commitNow {
            childFragmentManager.fragments.forEach { fragment ->
                if (fragment.tag != toShow) hide(fragment)
                else show(fragment)
            }
            setReorderingAllowed(true)
        }
    }
}