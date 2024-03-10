package dev.brahmkshatriya.echo.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItemsContainer
import dev.brahmkshatriya.echo.databinding.FragmentCategoryBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class CategoryFragment : Fragment() {
    private val viewModel: CategoryViewModel by activityViewModels()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.toolbarOutline.alpha = offset
        }

        binding.title.setupWithNavController(findNavController())

        val flow = viewModel.flow ?: return

        binding.title.title = viewModel.title
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        observe(flow) { data ->
            adapter.submit(
                data.map { it.toMediaItemsContainer() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.flow = null
        viewModel.title = null
    }

}