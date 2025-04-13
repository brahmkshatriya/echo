package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.media.MediaFragment
import dev.brahmkshatriya.echo.ui.media.more.MediaMoreBottomSheet
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.playlist.edit.search.EditPlaylistSearchClickListener
import dev.brahmkshatriya.echo.ui.shelf.ShelfFragment
import dev.brahmkshatriya.echo.ui.shelf.ShelfViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

open class ShelfClickListener(
    private val fragmentManager: FragmentManager,
    private val fragmentContainerId: Int = R.id.navHostFragment,
    private val onClick: ((opened: Boolean) -> Unit)? = null
) : ShelfAdapter.Listener {

    val fragment get() = fragmentManager.findFragmentById(fragmentContainerId)!!

    override fun onMoreClicked(extensionId: String?, shelf: Shelf.Lists<*>?, view: View) {
        val data = when (shelf) {
            is Shelf.Lists.Categories -> shelf.more
            is Shelf.Lists.Items -> shelf.more?.map { result ->
                result.getOrThrow().map { it.toShelf() }
            }

            is Shelf.Lists.Tracks -> shelf.more?.map { result ->
                result.getOrThrow().map { it.toMediaItem().toShelf() }
            }

            null -> null
        }
        openShelf(extensionId, shelf?.title ?: "", data ?: return, view)
        onClick?.invoke(true)
    }

    override fun onShuffleClicked(extensionId: String?, shelf: Shelf.Lists.Tracks?, view: View) {
        val list = shelf?.list ?: return
        onTrackClicked(extensionId, list, 0, null, view)
        val playerViewModel by fragment.activityViewModel<PlayerViewModel>()
        playerViewModel.setShuffle(true, changeCurrent = true)
    }

    override fun onMediaItemClicked(extensionId: String?, item: EchoMediaItem?, it: View?) {
        extensionId ?: return
        item ?: return
        fragment.openFragment<MediaFragment>(it, MediaFragment.getBundle(extensionId, item, false))
        onClick?.invoke(true)
    }

    override fun onMediaItemLongClicked(extensionId: String?, item: EchoMediaItem?, it: View) {
        extensionId ?: return
        item ?: return
        MediaMoreBottomSheet.newInstance(fragment.id, extensionId, item, false)
            .show(fragmentManager, null)
    }

    override fun onMediaItemPlayClicked(extensionId: String?, item: EchoMediaItem?, it: View) {
        val id = extensionId ?: return
        val mediaItem = item ?: return
        val playerViewModel by fragment.activityViewModel<PlayerViewModel>()
        playerViewModel.play(id, mediaItem, false)
    }

    override fun onCategoryClicked(extensionId: String?, category: Shelf.Category?, view: View) {
        extensionId ?: return
        category ?: return
        openShelf(extensionId, category.title, category.items ?: return, view)
        onClick?.invoke(true)
    }

    private fun openShelf(
        extensionId: String?,
        title: String,
        items: PagedData<out Shelf>,
        view: View
    ) {
        val activityVm by fragment.activityViewModel<ShelfViewModel>()
        activityVm.id = extensionId
        activityVm.title = title
        activityVm.data = items.map { it.getOrThrow() }
        fragment.openFragment<ShelfFragment>(view)
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
        val track = list.getOrNull(index) ?: return
        onMediaItemLongClicked(extensionId, track.toMediaItem(), view)
    }

    override fun onTrackSwiped(
        extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
    ) {
        val id = extensionId ?: return
        val track = list.getOrNull(index) ?: return
        val playerViewModel by fragment.activityViewModel<PlayerViewModel>()
        playerViewModel.addToNext(id, track.toMediaItem(), false)
    }

    companion object {
        fun Fragment.getShelfListener(): ShelfClickListener {
            val key = arguments?.getString("itemListener")
            return when (key) {
                "search" -> EditPlaylistSearchClickListener(parentFragmentManager)
                else -> ShelfClickListener(requireActivity().supportFragmentManager)
            }
        }
    }
}