package dev.brahmkshatriya.echo.ui.main.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentLibraryBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsetsMain
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.main.FeedViewModel.Companion.configureFeed
import dev.brahmkshatriya.echo.ui.main.MainFragment
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyPlayerBg
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.configureMainMenu
import dev.brahmkshatriya.echo.ui.playlist.CreatePlaylistBottomSheet
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LibraryFragment : Fragment() {
    private var binding by autoCleared<FragmentLibraryBinding>()
    private val viewModel by activityViewModel<LibraryViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyPlayerBg(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerViewLib, 96) {
            binding.fabContainer.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.toolBar.configureMainMenu(parentFragment as MainFragment)
        binding.appBarLayout.configureAppBar { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }

        val listener = configureFeed(
            viewModel, binding.recyclerViewLib, binding.swipeRefresh, binding.tabLayout
        )

        val parent = requireParentFragment()
        binding.fabCreatePlaylist.setOnClickListener {
            CreatePlaylistBottomSheet().show(parent.parentFragmentManager, null)
        }

        parent.parentFragmentManager.setFragmentResultListener("createPlaylist", this) { _, data ->
            val extensionId = data.getString("extensionId")
            val playlist = data.getSerialized<Playlist>("playlist")
            if (extensionId != null && playlist != null) createSnack(
                Message(
                    getString(R.string.x_created, playlist.title),
                    Message.Action(getString(R.string.view)) {
                        listener.onMediaItemClicked(extensionId, playlist.toMediaItem(), null)
                    }
                )
            )
            viewModel.refresh(true)
        }

        parent.parentFragmentManager.setFragmentResultListener("deleted", this) { _, _ ->
            viewModel.refresh(true)
        }

        parent.parentFragmentManager.setFragmentResultListener("reloadLibrary", this) { _, _ ->
            viewModel.refresh(true)
        }
    }
}