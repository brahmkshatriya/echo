package dev.brahmkshatriya.echo.ui.playlist.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentPlaylistEditBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsetsWithChild
import dev.brahmkshatriya.echo.ui.feed.TabsAdapter
import dev.brahmkshatriya.echo.ui.playlist.edit.EditPlaylistBottomSheet.Companion.toText
import dev.brahmkshatriya.echo.ui.playlist.edit.search.EditPlaylistSearchFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class EditPlaylistFragment : Fragment() {

    companion object {
        fun getBundle(extension: String, playlist: Playlist, loaded: Boolean) = Bundle().apply {
            putString("extensionId", extension)
            putSerialized("playlist", playlist)
            putBoolean("loaded", loaded)
        }
    }

    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }
    private val playlist by lazy { args.getSerialized<Playlist>("playlist")!! }
    private val loaded by lazy { args.getBoolean("loaded", false) }
    private val selectedTab by lazy { args.getString("selectedTabId").orEmpty() }

    private var binding: FragmentPlaylistEditBinding by autoCleared()
    private val vm by viewModel<EditPlaylistViewModel> {
        parametersOf(extensionId, playlist, loaded, selectedTab, -1)
    }

    private val adapter by lazy {
        val (listener, itemCallback) = PlaylistTrackAdapter.getTouchHelperAndListener(vm)
        itemCallback.attachToRecyclerView(binding.recyclerView)
        PlaylistTrackAdapter(listener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsWithChild(binding.appBarLayout, binding.recyclerView, 96) {
            binding.fabContainer.applyInsets(it)
        }

        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset ->
            binding.toolbarOutline.alpha = offset
            binding.toolbarIconContainer.alpha = 1 - offset
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            parentFragmentManager.setFragmentResult("delete", Bundle().apply {
                putSerialized("playlist", playlist)
            })
            parentFragmentManager.popBackStack()
            true
        }

        binding.save.setOnClickListener {
            vm.save()
        }
        observe(vm.isSaveable) {
            binding.save.isEnabled = it
        }

        binding.add.setOnClickListener {
            openFragment<EditPlaylistSearchFragment>(
                it, EditPlaylistSearchFragment.getBundle(extensionId)
            )
        }
        parentFragmentManager.setFragmentResultListener("searchedTracks", this) { _, bundle ->
            val tracks = bundle.getSerialized<List<Track>>("tracks")!!.toMutableList()
            vm.edit(
                EditPlaylistViewModel.Action.Add(
                    vm.currentTracks.value?.size ?: 0, tracks
                )
            )
        }

        FastScrollerHelper.applyTo(binding.recyclerView)

        val headerAdapter = EditPlaylistHeaderAdapter(vm)
        val tabAdapter = TabsAdapter<Tab>({ title }) { v, index, tab ->
            vm.selectedTabFlow.value = tab
        }

        binding.recyclerView.adapter = ConcatAdapter(headerAdapter, tabAdapter, adapter)
        observe(vm.pairFlow) { headerAdapter.pair = it }
        observe(vm.tabsFlow) { tabAdapter.data = it }
        observe(vm.selectedTabFlow) { tabAdapter.selected = vm.tabsFlow.value.indexOf(it) }
        observe(vm.currentTracks) { adapter.submitList(it) }

        val combined = vm.originalList.combine(vm.saveState) { a, b -> a to b }
        observe(combined) { (tracks, save) ->
            val trackLoading = tracks == null
            val saving = save != EditPlaylistViewModel.SaveState.Initial
            val loading = trackLoading || saving
            binding.recyclerView.isVisible = !loading
            binding.fabContainer.isVisible = !loading
            binding.loading.root.isVisible = loading
            binding.loading.textView.text = save.toText(playlist, requireContext())

            val save = save as? EditPlaylistViewModel.SaveState.Saved ?: return@observe
            if (save.result.isSuccess) parentFragmentManager.setFragmentResult(
                "reload", bundleOf("id" to playlist.id)
            )
            parentFragmentManager.popBackStack()
        }
    }
}