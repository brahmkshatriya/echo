package dev.brahmkshatriya.echo.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsetsMain
import dev.brahmkshatriya.echo.ui.main.MainFragment
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.configureMainMenu
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SearchFragment : Fragment() {
    private val parent get() = parentFragment as Fragment
    var binding by autoCleared<FragmentSearchBinding>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView) {
            binding.quickSearchView.updatePaddingRelative(start = it.start, end = it.end)
            binding.quickSearchRecyclerView.updatePaddingRelative(bottom = it.bottom)
        }
        applyBackPressCallback {
            if (it == STATE_EXPANDED) binding.quickSearchView.hide()
        }
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.searchBar.alpha = 1 - offset
            binding.toolBar.alpha = 1 - offset
        }
        val main = parent as? MainFragment
        if (main != null) binding.toolBar.configureMainMenu(main)
        else {
            binding.toolBar.isVisible = false
            binding.searchBar.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 24.dpToPx(requireContext())
            }
        }
    }
}