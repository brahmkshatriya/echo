package dev.brahmkshatriya.echo.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentLibraryBinding
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.MainFragment
import dev.brahmkshatriya.echo.ui.MainFragment.Companion.first
import dev.brahmkshatriya.echo.ui.MainFragment.Companion.scrollTo
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.ui.common.configureFeedUI
import dev.brahmkshatriya.echo.ui.common.configureMainMenu
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsetsMain

class LibraryFragment : Fragment() {

    private var binding by autoCleared<FragmentLibraryBinding>()
    private val viewModel by activityViewModels<LibraryViewModel>()
    private val parent get() = parentFragment as MainFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView) {
            binding.fabContainer.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.toolBar.configureMainMenu(parent)
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }

        configureFeedUI<LibraryFeedClient>(
            R.string.library,
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

        observe(viewModel.extensionFlow) {
            binding.fabCreatePlaylist.isVisible = it?.isClient<PlaylistEditClient>() ?: false
        }

        binding.fabCreatePlaylist.setOnClickListener {
            CreatePlaylistBottomSheet().show(parentFragmentManager, null)
        }

        val listener = ShelfAdapter.getListener(parent)
        parentFragmentManager.setFragmentResultListener("createPlaylist", this) { _, data ->
            val clientId = data.getString("extensionId")
            val playlist = data.getSerialized<Playlist>("playlist")
            if (clientId != null && playlist != null) createSnack(Message(
                getString(R.string.playlist_created, playlist.title),
                Message.Action(getString(R.string.view)) {
                    listener.onClick(clientId, playlist.toMediaItem(), null)
                }
            ))
            viewModel.refresh(true)
        }

        parent.parentFragmentManager.setFragmentResultListener("deleted", this) { _, _ ->
            viewModel.refresh(true)
        }

        parent.parentFragmentManager.setFragmentResultListener("reloadLibrary", this) { _, _ ->
            viewModel.refresh(true)
        }
    }

    override fun onStop() {
        viewModel.recyclerPosition = binding.recyclerView.first()
        super.onStop()
    }
}