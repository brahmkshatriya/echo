package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Buttons.Companion.EMPTY
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.FragmentLibraryBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.cache.Cached
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.configure
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getFeedAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getTouchHelper
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener.Companion.getFeedListener
import dev.brahmkshatriya.echo.ui.feed.FeedData
import dev.brahmkshatriya.echo.ui.feed.FeedViewModel
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.playlist.create.CreatePlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.settings.SettingsBottomSheet
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LibraryFragment : Fragment(R.layout.fragment_library) {
    private val feedData by lazy {
        val vm by viewModel<FeedViewModel>()
        val id = "library"
        vm.getFeedData(id, EMPTY, cached = {
            val curr = current.value!!
            val feed = Cached.getFeedShelf(app, curr.id, id).getOrThrow()
            FeedData.State(curr.id, null, feed)
        }) {
            val curr = current.value!!
            val feed = Cached.savingFeed(
                app, curr, id,
                curr.getAs<LibraryFeedClient, Feed<Shelf>> { loadLibraryFeed() }.getOrThrow()
            )
            FeedData.State(curr.id, null, feed)
        }
    }

    private val listener by lazy { getFeedListener(requireParentFragment()) }
    private val feedAdapter by lazy { getFeedAdapter(feedData, listener) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLibraryBinding.bind(view)
        setupTransition(view, false, MaterialSharedAxis.Y)
        val headerAdapter = HeaderAdapter(this)
        val uiViewModel by activityViewModel<UiViewModel>()
        observe(uiViewModel.navigationReselected) {
            if (it != 2) return@observe
            SettingsBottomSheet().show(parentFragmentManager, null)
        }
        observe(
            uiViewModel.navigation.combine(feedData.backgroundImageFlow) { a, b -> a to b }
        ) { (curr, bg) ->
            if (curr != 2) return@observe
            uiViewModel.currentNavBackground.value = bg
        }
        applyInsets(binding.recyclerView, binding.appBarOutline, 72) {
            binding.createPlaylistContainer.applyInsets(it)
            binding.swipeRefresh.configure(it)
        }
        applyBackPressCallback()
        getTouchHelper(listener).attachToRecyclerView(binding.recyclerView)
        configureGridLayout(
            binding.recyclerView,
            feedAdapter.withLoading(this, headerAdapter)
        )
        binding.swipeRefresh.run {
            setOnRefreshListener { feedData.refresh() }
            observe(feedData.isRefreshingFlow) {
                isRefreshing = it
            }
        }

        observe(feedData.current) {
            binding.createPlaylist.isVisible = it?.isClient<PlaylistEditClient>() ?: false
        }
        val parent = requireParentFragment()
        binding.createPlaylist.setOnClickListener {
            CreatePlaylistBottomSheet().show(parent.parentFragmentManager, null)
        }

        parent.parentFragmentManager.setFragmentResultListener("createPlaylist", this) { _, data ->
            val extensionId = data.getString("extensionId")
            val playlist = data.getSerialized<Playlist>("playlist")
            if (extensionId != null && playlist != null) createSnack(
                Message(
                    getString(R.string.x_created, playlist.title),
                    Message.Action(getString(R.string.view)) {
                        listener.onMediaClicked(null, extensionId, playlist, null)
                    }
                )
            )
            feedData.refresh()
        }

        parent.parentFragmentManager.setFragmentResultListener("deleted", this) { _, _ ->
            feedData.refresh()
        }

        parent.parentFragmentManager.setFragmentResultListener("reloadLibrary", this) { _, _ ->
            feedData.refresh()
        }
    }
}