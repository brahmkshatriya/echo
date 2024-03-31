package dev.brahmkshatriya.echo.newui.media

import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.newui.item.ItemFragmentDirections
import dev.brahmkshatriya.echo.viewmodels.SnackBarViewModel.Companion.createSnack

class MediaClickListener(
    private val fragment: Fragment
) : MediaItemAdapter.Listener {

    override fun onClick(clientId: String?, item: EchoMediaItem, transitionView: View) {
        clientId ?: return fragment.createSnack(R.string.error_no_client)
        val action = ItemFragmentDirections.actionItem(item, clientId)
        val extras =
            FragmentNavigatorExtras(transitionView to transitionView.transitionName)
        fragment.findNavController().navigate(action, extras)
    }

    override fun onLongClick(
        clientId: String?,
        item: EchoMediaItem,
        transitionView: View
    ): Boolean {
        println("Long Clicked : ${item.title}")
        return true
    }

    fun onClick(clientId: String?, item: MediaItemsContainer, transitionView: View) {
        when (item) {
            is MediaItemsContainer.Category -> {
                println("Category : ${item.title}")
            }

            is MediaItemsContainer.Item -> {
                onClick(clientId, item.media, transitionView)
            }
        }
    }

    fun onLongClick(
        clientId: String?, item: MediaItemsContainer, transitionView: View
    ) = when (item) {
        is MediaItemsContainer.Category -> {
            onClick(clientId, item, transitionView)
            true
        }

        is MediaItemsContainer.Item -> {
            onLongClick(clientId, item.media, transitionView)
        }
    }


}