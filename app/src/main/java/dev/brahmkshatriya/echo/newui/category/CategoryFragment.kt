package dev.brahmkshatriya.echo.newui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.FragmentCategoryBinding
import dev.brahmkshatriya.echo.newui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.newui.media.MediaContainerLoadingAdapter.Companion.withLoaders
import dev.brahmkshatriya.echo.utils.Animator.setupTransition
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CategoryFragment : Fragment() {

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    private var binding by autoCleared<FragmentCategoryBinding>()
    private val args by navArgs<CategoryFragmentArgs>()
    private val transferVModel by activityViewModels<VModel>()

    private val viewModel by viewModels<VModel>()
    private val adapter = MediaContainerAdapter(this)
    private val concatAdapter = adapter.withLoaders()

    class VModel : ViewModel() {
        var category: MediaItemsContainer.Category? = null
        val flow = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    }

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

        adapter.clientId = args.clientId
        binding.recyclerView.adapter = concatAdapter

        val category = if (viewModel.category == null) {
            val category = transferVModel.category ?: return
            viewModel.category = category
            viewModel.viewModelScope.launch {
                category.more
                    ?.cachedIn(viewModel.viewModelScope)
                    ?.catchWith(throwableFlow)
                    ?.collect { data ->
                        viewModel.flow.value = data.map { it.toMediaItemsContainer() }
                    }
            }
            transferVModel.category = null
            category
        } else viewModel.category ?: return

        val transitionName = category.hashCode().toString()
        binding.root.transitionName = transitionName

        binding.toolBar.title = category.title

        observe(viewModel.flow) { data ->
            adapter.submit(data)
        }
    }
}