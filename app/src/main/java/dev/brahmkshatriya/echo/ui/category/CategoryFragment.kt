package dev.brahmkshatriya.echo.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.FragmentCategoryBinding
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.media.MediaContainerLoadingAdapter.Companion.withLoaders
import dev.brahmkshatriya.echo.utils.Animator.setupTransition
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

@AndroidEntryPoint
class CategoryFragment : Fragment() {

    private var binding by autoCleared<FragmentCategoryBinding>()
    private val clientId by lazy {
        requireArguments().getString("clientId")!!
    }

    private val activityViewModel by activityViewModels<CategoryViewModel>()
    private val viewModel by viewModels<CategoryViewModel>()

    private val adapter = MediaContainerAdapter(this)
    private val concatAdapter = adapter.withLoaders()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupTransition(binding.root)
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolBar.setupWithNavController(findNavController())

        adapter.clientId = clientId
        binding.recyclerView.adapter = concatAdapter

        val category = if (viewModel.category == null) {
            val category = activityViewModel.category ?: return
            activityViewModel.category = null
            viewModel.category = category
            viewModel.initialize()
            category
        } else viewModel.category ?: return

        val transitionName = category.hashCode().toString()
        binding.root.transitionName = transitionName

        binding.toolBar.title = category.title

        observe(viewModel.flow) { data ->
            adapter.submit(data)
        }
    }

    companion object {
        fun newInstance(clientId: String): Fragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", clientId)
                }
            }
        }
    }
}