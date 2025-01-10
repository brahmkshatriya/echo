package dev.brahmkshatriya.echo.ui.container

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.FragmentCategoryBinding
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.Job

@AndroidEntryPoint
class ContainerFragment : Fragment() {

    private var binding by autoCleared<FragmentCategoryBinding>()
    private val clientId by lazy {
        requireArguments().getString("clientId")!!
    }
    private val title by lazy {
        requireArguments().getString("title")!!
    }

    private val activityViewModel by activityViewModels<ContainerViewModel>()
    private val viewModel by viewModels<ContainerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        FastScrollerHelper.applyTo(binding.recyclerView)

        if (viewModel.moreFlow == null) {
            val category = activityViewModel.moreFlow ?: return
            activityViewModel.moreFlow = null
            viewModel.moreFlow = category
            viewModel.initialize()
        }

        binding.toolBar.title = title

        var shelfAdapter: ShelfAdapter? = null
        var job: Job? = null
        observe(activityViewModel.extensionListFlow) { list ->
            val extension = list?.find { it.metadata.id == clientId }
            extension ?: return@observe
            val adapter = ShelfAdapter(this, view.transitionName, extension)
            job?.cancel()
            job = adapter.applyCurrent(this, binding.recyclerView)
            val concatAdapter = adapter.withLoaders()
            shelfAdapter = adapter
            binding.recyclerView.adapter = concatAdapter
        }

        observe(viewModel.flow) { data ->
            shelfAdapter?.submit(data)
        }
    }

    companion object {
        fun newInstance(clientId: String, title: String): Fragment {
            return ContainerFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", clientId)
                    putString("title", title)
                }
            }
        }
    }
}