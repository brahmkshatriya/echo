package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.emit
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ShelfClickListener(
    private val fragmentManager: FragmentManager,
    private val fragmentContainerId: Int = R.id.navHostFragment,
    private val onClick: ((opened: Boolean) -> Unit)? = null
) : ShelfAdapter.Listener {

    val fragment get() = fragmentManager.findFragmentById(fragmentContainerId)!!

    private fun todo() {
        val fragment = fragment
        val app by fragment.inject<App>()
        fragment.emit(app.messageFlow, Message("Not yet implemented"))
    }

    override fun onMoreClicked(extensionId: String?, shelf: Shelf.Lists<*>?, view: View) {
        todo()
        onClick?.invoke(true)
    }

    override fun onShuffleClicked(extensionId: String?, shelf: Shelf.Lists.Tracks?, view: View) {
        val list = shelf?.list ?: return
        onTrackClicked(extensionId, list, 0, null, view)
        val playerViewModel by fragment.activityViewModel<PlayerViewModel>()
        playerViewModel.setShuffle(true, changeCurrent = true)
    }

    override fun onShelfSearchClicked(extensionId: String?, shelf: PagingData<Shelf>?, view: View) {
        todo()
    }

    override fun onShelfSortClicked(extensionId: String?, shelf: PagingData<Shelf>?, view: View) {
        todo()
    }

    override fun onMediaItemClicked(extensionId: String?, item: EchoMediaItem?, it: View) {
        when (item) {
            is EchoMediaItem.Lists.AlbumItem -> todo()
            is EchoMediaItem.Lists.PlaylistItem -> todo()
            is EchoMediaItem.Lists.RadioItem -> todo()
            is EchoMediaItem.Profile.ArtistItem -> todo()
            is EchoMediaItem.Profile.UserItem -> todo()
            is EchoMediaItem.TrackItem -> {
                return onTrackClicked(extensionId, listOf(item.track), 0, null, it)
            }

            null -> Unit
        }
        onClick?.invoke(true)
    }

    override fun onMediaItemLongClicked(extensionId: String?, item: EchoMediaItem?, it: View) {
        when (item) {
            is EchoMediaItem.Lists.AlbumItem -> todo()
            is EchoMediaItem.Lists.PlaylistItem -> todo()
            is EchoMediaItem.Lists.RadioItem -> todo()
            is EchoMediaItem.Profile.ArtistItem -> todo()
            is EchoMediaItem.Profile.UserItem -> todo()
            is EchoMediaItem.TrackItem ->
                onTrackLongClicked(extensionId, listOf(item.track), 0, null, it)

            null -> Unit
        }
    }

    override fun onCategoryClicked(extensionId: String?, category: Shelf.Category?, view: View) {
        todo()
        onClick?.invoke(true)
    }

    override fun onCategoryLongClicked(
        extensionId: String?, category: Shelf.Category?, view: View
    ) {
        onCategoryClicked(extensionId, category, view)
    }

    override fun onTrackClicked(
        extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
    ) {
        val id = extensionId ?: return
        val playerViewModel by fragment.activityViewModel<PlayerViewModel>()
        playerViewModel.setQueue(id, list, index, context)
        playerViewModel.setPlaying(true)
        onClick?.invoke(false)
    }

    override fun onTrackLongClicked(
        extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
    ) {
        todo()
    }
}