package dev.brahmkshatriya.echo.newui.media

import android.view.View
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

class MediaClickListener(
    private val fragment: Fragment
) : MediaItemAdapter.Listener {

    override fun onClick(item: EchoMediaItem, transitionView: View) {
        println("Clicked : ${item.title}")
    }

    override fun onLongClick(item: EchoMediaItem, transitionView: View): Boolean {
        println("Long Clicked : ${item.title}")
        return true
    }

    fun onClick(item: MediaItemsContainer, transitionView: View) {
        when (item) {
            is MediaItemsContainer.Category -> {
                println("Category : ${item.title}")
            }

            is MediaItemsContainer.Item -> {
                onClick(item.media, transitionView)
            }
        }
    }

    fun onLongClick(item: MediaItemsContainer, transitionView: View): Boolean {
        return when (item) {
            is MediaItemsContainer.Category -> {
                onClick(item, transitionView)
                true
            }

            is MediaItemsContainer.Item -> {
                onLongClick(item.media, transitionView)
            }
        }
    }

}