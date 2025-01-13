package dev.brahmkshatriya.echo.ui.adapter

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Shelf.Category
import dev.brahmkshatriya.echo.common.models.Shelf.Item
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.container.ContainerFragment
import dev.brahmkshatriya.echo.ui.container.ContainerViewModel
import dev.brahmkshatriya.echo.ui.item.ItemBottomSheet
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.radioNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

open class ShelfClickListener(
    private val fragmentManager: FragmentManager,
    private val fragmentId: Int = R.id.navHostFragment,
    private val afterOpening: (() -> Unit)? = null,
) : ShelfAdapter.Listener {

    val fragment get() = fragmentManager.findFragmentById(fragmentId)!!

    private fun snack(block: Context.() -> SnackBar.Message) =
        fragment.createSnack(fragment.requireContext().block())

    private inline fun <reified T> withClient(
        clientId: String, crossinline block: (PlayerViewModel) -> Unit
    ) {
        val playerViewModel by fragment.activityViewModels<PlayerViewModel>()
        val extension = playerViewModel.extensionListFlow.getExtension(clientId)
            ?: return snack { noClient() }
        playerViewModel.viewModelScope.launch {
            if (!extension.isClient<T>()) return@launch snack {
                when (T::class) {
                    TrackClient::class -> trackNotSupported(clientId)
                    RadioClient::class -> radioNotSupported(clientId)
                    else -> noClient()
                }
            }
            block(playerViewModel)
        }
    }

    private fun openItem(clientId: String, item: EchoMediaItem, transitionView: View?) {
        fragment.openFragment(ItemFragment.newInstance(clientId, item), transitionView)
        afterOpening?.invoke()
    }

    private fun openContainer(
        clientId: String, title: String,
        flow: Flow<PagingData<Shelf>>?, transitionView: View?
    ) {
        flow ?: return
        val viewModel by fragment.activityViewModels<ContainerViewModel>()
        viewModel.moreFlow = flow
        fragment.openFragment(ContainerFragment.newInstance(clientId, title), transitionView)
        afterOpening?.invoke()
    }

    override fun onClick(
        clientId: String, item: EchoMediaItem, transitionView: View?
    ) {
        when (item) {
            is EchoMediaItem.TrackItem -> {
                withClient<TrackClient>(clientId) {
                    it.play(clientId, item.track, 0)
                }
            }

            is EchoMediaItem.Lists.RadioItem -> {
                if (item.radio.tabs.isNotEmpty())
                    return openItem(clientId, item, transitionView)
                withClient<RadioClient>(clientId) {
                    it.play(clientId, item, 0)
                }
            }

            else -> openItem(clientId, item, transitionView)
        }
    }

    override fun onClick(
        clientId: String,
        context: EchoMediaItem?,
        list: List<Track>,
        pos: Int,
        view: View
    ) = withClient<TrackClient>(clientId) {
        it.play(clientId, context, list, pos)
    }

    override fun onLongClick(
        clientId: String,
        context: EchoMediaItem?,
        list: List<Track>,
        pos: Int,
        view: View
    ): Boolean {
        val track = list[pos]
        return onLongClick(clientId, track.toMediaItem(), view)
    }

    override fun onShuffleClick(clientId: String, shelf: Shelf.Lists.Tracks) {
        withClient<TrackClient>(clientId) {
            it.play(clientId, null, shelf.list)
            it.withBrowser { browser -> browser.shuffleModeEnabled = true }
        }
    }

    override fun onTrackSwiped(
        clientId: String, context: EchoMediaItem?, list: List<Track>, pos: Int, view: View
    ) {
        val track = list[pos]
        withClient<TrackClient>(clientId) {
            it.addToQueue(clientId, track, true)
            snack { SnackBar.Message(getString(R.string.track_added_to_queue, track.title)) }
        }
    }

    override fun onLongClick(
        clientId: String, item: EchoMediaItem, transitionView: View?
    ): Boolean {
        ItemBottomSheet.newInstance(clientId, item, loaded = false, fromPlayer = false)
            .show(fragmentManager, null)
        return true
    }

    override fun onClick(clientId: String, shelf: Shelf, transitionView: View) {
        when (shelf) {
            is Item -> onClick(clientId, shelf.media, transitionView)
            is Category -> openContainer(
                clientId,
                shelf.title,
                shelf.items?.toFlow(),
                transitionView
            )

            is Shelf.Lists.Tracks -> openContainer(
                clientId,
                shelf.title,
                shelf.more?.toFlow()?.map { it.map { track -> track.toMediaItem().toShelf() } },
                transitionView
            )

            is Shelf.Lists.Items -> openContainer(
                clientId,
                shelf.title,
                shelf.more?.toFlow()?.map { it.map { item -> item.toShelf() } },
                transitionView
            )

            is Shelf.Lists.Categories -> openContainer(
                clientId,
                shelf.title,
                shelf.more?.toFlow()?.map { it.map { category -> category } },
                transitionView
            )
        }
    }

    override fun onLongClick(
        clientId: String, shelf: Shelf, transitionView: View
    ) = when (shelf) {
        is Item -> onLongClick(clientId, shelf.media, transitionView)
        else -> {
            onClick(clientId, shelf, transitionView)
            true
        }
    }
}