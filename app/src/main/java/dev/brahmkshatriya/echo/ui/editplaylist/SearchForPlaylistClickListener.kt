package dev.brahmkshatriya.echo.ui.editplaylist

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.paging.PagingData
import androidx.paging.map
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Category
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Container
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Item
import dev.brahmkshatriya.echo.ui.adapter.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.adapter.MediaItemAdapter
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.container.ContainerFragment
import dev.brahmkshatriya.echo.ui.container.ContainerViewModel
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchForPlaylistClickListener(
    private val fragmentManager: FragmentManager
) : MediaContainerAdapter.Listener, MediaItemAdapter.Listener {

    val fragment get() = fragmentManager.findFragmentById(R.id.playlistSearchContainer)!!

    private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())
    private fun openItem(clientId: String?, item: EchoMediaItem, transitionView: View?) {
        clientId ?: return noClient()
        fragment.openFragment(ItemFragment.newInstance(clientId, item), transitionView)
    }

    private fun openContainer(
        clientId: String?, title: String,
        flow: Flow<PagingData<MediaItemsContainer>>?, transitionView: View?
    ) {
        clientId ?: return noClient()
        flow ?: return
        val viewModel by fragment.activityViewModels<ContainerViewModel>()
        viewModel.moreFlow = flow
        fragment.openFragment(ContainerFragment.newInstance(clientId, title), transitionView)
    }

    override fun onClick(
        clientId: String?, item: EchoMediaItem, transitionView: View?
    ) {
        when (item) {
            is EchoMediaItem.TrackItem -> {
                clientId ?: return noClient()
                val viewModel by fragment.activityViewModels<SearchForPlaylistViewModel>()
                viewModel.addTrack(item.track)
            }

            else -> openItem(clientId, item, transitionView)
        }
    }

    override fun onLongClick(
        clientId: String?, item: EchoMediaItem, transitionView: View?
    ): Boolean {
        onClick(clientId, item, transitionView)
        return true
    }

    override fun onClick(clientId: String?, container: MediaItemsContainer, transitionView: View) {
        when (container) {
            is Item -> onClick(clientId, container.media, transitionView)
            is Category -> openContainer(
                clientId,
                container.title,
                container.more?.toFlow()?.map { it.map { item -> item.toMediaItemsContainer() } },
                transitionView
            )

            is Container -> openContainer(
                clientId,
                container.title,
                container.more?.toFlow(),
                transitionView
            )

            is MediaItemsContainer.Tracks -> openContainer(
                clientId,
                container.title,
                container.more?.toFlow()?.map { it.map { item -> item.toMediaItem().toMediaItemsContainer() } },
                transitionView
            )
        }
    }

    override fun onLongClick(
        clientId: String?, container: MediaItemsContainer, transitionView: View
    ) = when (container) {
        is Item -> onLongClick(clientId, container.media, transitionView)
        else -> {
            onClick(clientId, container, transitionView)
            true
        }
    }
}