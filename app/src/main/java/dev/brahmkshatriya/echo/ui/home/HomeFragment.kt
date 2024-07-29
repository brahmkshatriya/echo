package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentHomeBinding
import dev.brahmkshatriya.echo.ui.common.MainFragment
import dev.brahmkshatriya.echo.ui.common.MainFragment.Companion.first
import dev.brahmkshatriya.echo.ui.common.MainFragment.Companion.scrollTo
import dev.brahmkshatriya.echo.ui.common.configureFeedUI
import dev.brahmkshatriya.echo.ui.common.configureMainMenu
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsetsMain

class HomeFragment : Fragment() {

    private var binding by autoCleared<FragmentHomeBinding>()
    private val viewModel by activityViewModels<HomeFeedViewModel>()
    private val parent get() = parentFragment as MainFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView)
        applyBackPressCallback()
        binding.toolBar.configureMainMenu(parent)
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }

        configureFeedUI(
            R.string.home,
            viewModel,
            binding.recyclerView,
            binding.swipeRefresh,
            binding.tabLayout
        )

        binding.recyclerView.scrollTo(viewModel.recyclerPosition) {
            binding.appBarLayout.setExpanded(it < 1, false)
        }
        view.doOnLayout {
            binding.appBarOutline.alpha = 0f
        }
    }

    override fun onStop() {
        viewModel.recyclerPosition = binding.recyclerView.first()
        super.onStop()
    }
}