package dev.brahmkshatriya.echo.ui.category

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItemsContainer
import dev.brahmkshatriya.echo.databinding.FragmentCategoryBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class CategoryFragment : Fragment() {
    private val transferViewModel: CategoryViewModel by activityViewModels()
    private val viewModel: CategoryViewModel by viewModels()
    private var binding: FragmentCategoryBinding by autoCleared()

    private val adapter = MediaItemsContainerAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        enterTransition = MaterialFade()
        exitTransition = MaterialFade()
        return binding.root
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.toolbarOutline.alpha = offset
        }

        binding.title.setupWithNavController(findNavController())

        //Transfer data from transferViewModel to viewModel
        if(viewModel.flow == null) {
            transferViewModel.title?.let {
                viewModel.title = it
                transferViewModel.title = null
            }
            transferViewModel.flow?.let {
                viewModel.flow = it
                transferViewModel.flow = null
            }
            viewModel.flow?.cachedIn(viewModel.viewModelScope)
        }

        val flow = viewModel.flow ?: return
        binding.root.transitionName = flow.hashCode().toString()
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }

        binding.title.title = viewModel.title
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        observe(flow) { data ->
            adapter.submit(
                data.map { it.toMediaItemsContainer() }
            )
        }
    }
}