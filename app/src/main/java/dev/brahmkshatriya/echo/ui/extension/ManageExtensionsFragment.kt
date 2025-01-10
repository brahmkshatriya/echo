package dev.brahmkshatriya.echo.ui.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.FragmentManageExtensionsBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.ui.configure
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsetsMain
import kotlinx.coroutines.Job

class ManageExtensionsFragment : Fragment() {
    var binding by autoCleared<FragmentManageExtensionsBinding>()
    val viewModel by activityViewModels<ExtensionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentManageExtensionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView) {
            binding.fabContainer.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        FastScrollerHelper.applyTo(binding.recyclerView)
        val refresh = binding.toolBar.findViewById<View>(R.id.menu_refresh)
        refresh.setOnClickListener { viewModel.refresh() }
        binding.swipeRefresh.configure { viewModel.refresh() }

        binding.fabAddExtensions.setOnClickListener {
            ExtensionsAddListBottomSheet.LinkFile().show(parentFragmentManager, null)
        }

        var type = ExtensionType.entries[binding.tabLayout.selectedTabPosition]
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.moveExtensionItem(type, toPos, fromPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun getMovementFlags(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ) = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        }

        val touchHelper = ItemTouchHelper(callback)
        val extensionAdapter = ExtensionAdapter(object : ExtensionAdapter.Listener {
            override fun onClick(extension: Extension<*>, view: View) {
                openFragment(ExtensionInfoFragment.newInstance(extension), view)
            }

            override fun onDragHandleTouched(viewHolder: ExtensionAdapter.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }
        })

        fun change(pos: Int): Job {
            type = ExtensionType.entries[pos]
            val flow = viewModel.getExtensionListFlow(type)
            return observe(flow) { list ->
                extensionAdapter.submit(list ?: emptyList())
            }
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            var job: Job? = null
            override fun onTabSelected(tab: TabLayout.Tab) {
                job?.cancel()
                job = change(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()
        touchHelper.attachToRecyclerView(binding.recyclerView)

        observe(viewModel.refresher) {
            change(binding.tabLayout.selectedTabPosition)
            binding.swipeRefresh.isRefreshing = it
        }
    }
}