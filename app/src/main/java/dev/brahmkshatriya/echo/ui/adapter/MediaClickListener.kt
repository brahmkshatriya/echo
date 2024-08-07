package dev.brahmkshatriya.echo.ui.adapter

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.paging.PagingData
import androidx.paging.map
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Category
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Container
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Item
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.container.ContainerFragment
import dev.brahmkshatriya.echo.ui.container.ContainerViewModel
import dev.brahmkshatriya.echo.ui.item.ItemBottomSheet
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaClickListener(
    private val fragmentManager: FragmentManager, private val afterOpening: (() -> Unit)? = null
) : MediaContainerAdapter.Listener {

    val fragment get() = fragmentManager.findFragmentById(R.id.navHostFragment)!!

    private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())
    private fun trackNotSupported(client: String) =
        fragment.createSnack(fragment.requireContext().trackNotSupported(client))

    private fun openItem(clientId: String?, item: EchoMediaItem, transitionView: View?) {
        clientId ?: return noClient()
        fragment.openFragment(ItemFragment.newInstance(clientId, item), transitionView)
        afterOpening?.invoke()
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
        afterOpening?.invoke()
    }

    override fun onClick(
        clientId: String?, item: EchoMediaItem, transitionView: View?
    ) {
        when (item) {
            is EchoMediaItem.TrackItem -> {
                clientId ?: return noClient()
                val playerViewModel by fragment.activityViewModels<PlayerViewModel>()
                val extension = playerViewModel.extensionListFlow.getExtension(clientId)
                    ?: return noClient()
                if (extension.client !is TrackClient)
                    return trackNotSupported(extension.metadata.name)
                playerViewModel.play(clientId, item.track, 0)
            }

            else -> openItem(clientId, item, transitionView)
        }
    }

    override fun onLongClick(
        clientId: String?, item: EchoMediaItem, transitionView: View?
    ): Boolean {
        clientId ?: return run { noClient();false }
        ItemBottomSheet.newInstance(clientId, item, loaded = false, fromPlayer = false)
            .show(fragmentManager, null)
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