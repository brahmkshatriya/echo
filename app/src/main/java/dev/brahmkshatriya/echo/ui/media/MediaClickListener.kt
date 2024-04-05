package dev.brahmkshatriya.echo.ui.media

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Category
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer.Item
import dev.brahmkshatriya.echo.ui.category.CategoryFragment
import dev.brahmkshatriya.echo.ui.category.CategoryViewModel
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBarViewModel.Companion.createSnack

class MediaClickListener(
    private val fragment: Fragment
) : MediaItemAdapter.Listener {

    private fun noClient() = fragment.createSnack(fragment.requireContext().noClient())

    private fun openItem(clientId: String?, item: EchoMediaItem, transitionView: View) {
        clientId ?: return noClient()
        fragment.openFragment(ItemFragment.newInstance(clientId, item), transitionView)
    }

    private fun openCategory(clientId: String?, category: Category, transitionView: View): Boolean {
        clientId ?: return noClient().let { true }
        val viewModel by fragment.activityViewModels<CategoryViewModel>()
        viewModel.category = category
        fragment.openFragment(CategoryFragment.newInstance(clientId), transitionView)
        return true
    }

    override fun onClick(clientId: String?, item: EchoMediaItem, transitionView: View) {
        when (item) {
            is EchoMediaItem.TrackItem -> {
                clientId ?: return noClient()
                val playerViewModel by fragment.activityViewModels<PlayerViewModel>()
                playerViewModel.play(clientId, item.track, 0)
            }

            else -> openItem(clientId, item, transitionView)
        }
    }

    override fun onLongClick(
        clientId: String?,
        item: EchoMediaItem,
        transitionView: View
    ): Boolean {
        openItem(clientId, item, transitionView)
        return true
    }

    fun onClick(clientId: String?, item: MediaItemsContainer, transitionView: View) {
        when (item) {
            is Category -> openCategory(clientId, item, transitionView)
            is Item -> onClick(clientId, item.media, transitionView)
        }
    }

    fun onLongClick(
        clientId: String?, item: MediaItemsContainer, transitionView: View
    ) = when (item) {
        is Category -> openCategory(clientId, item, transitionView)
        is Item -> onLongClick(clientId, item.media, transitionView)
    }
}