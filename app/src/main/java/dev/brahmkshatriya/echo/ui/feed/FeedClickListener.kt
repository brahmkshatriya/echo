package dev.brahmkshatriya.echo.ui.feed

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.media.MediaFragment
import dev.brahmkshatriya.echo.ui.media.MediaFragment.Companion.getBundle
import dev.brahmkshatriya.echo.ui.media.more.MediaMoreBottomSheet
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.playlist.edit.search.EditPlaylistSearchClickListener
import org.koin.androidx.viewmodel.ext.android.viewModel

open class FeedClickListener(
    private val fragment: Fragment,
    val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val afterOpen: () -> Unit = {}
) {
    companion object {
        fun Fragment.getFeedListener(
            navFragment: Fragment = this,
            afterOpen: () -> Unit = {}
        ): FeedClickListener {
            val key = arguments?.getString("feedListener")
            return when (key) {
                "playlist_search" -> EditPlaylistSearchClickListener(this)
                else -> FeedClickListener(this, navFragment.parentFragmentManager, navFragment.id, afterOpen)
            }
        }
    }

    open fun onTabSelected(
        view: View?,
        feedId: String?,
        extensionId: String?,
        position: Int
    ): Boolean {
        val vm by fragment.viewModel<FeedViewModel>()
        val feedData = vm.feedDataMap[feedId] ?: return notFoundSnack(R.string.feed)
        feedData.selectTab(extensionId, position)
        return true
    }

    open fun onSortClicked(view: View?, feedId: String?): Boolean {
        val vm by fragment.viewModel<FeedViewModel>()
        val feedData = vm.feedDataMap[feedId] ?: return notFoundSnack(R.string.feed)
        feedData.feedSortState.value = feedData.feedSortState.value ?: FeedSort.State()
        FeedSortBottomSheet.newInstance(feedId!!).show(fragment.childFragmentManager, null)
        return true
    }

    open fun onPlayClicked(
        view: View?,
        extensionId: String?,
        context: EchoMediaItem?,
        tracks: List<Track>?,
        shuffle: Boolean
    ): Boolean {
        if (extensionId == null) return notFoundSnack(R.string.extension)
        val vm by fragment.activityViewModels<PlayerViewModel>()
        if (tracks != null) {
            if (tracks.isEmpty()) return notFoundSnack(R.string.tracks)
            vm.setQueue(extensionId, tracks, 0, context)
            vm.setShuffle(shuffle, true)
            vm.setPlaying(true)
            return true
        }
        if (context == null) return notFoundSnack(R.string.item)
        if (shuffle) vm.shuffle(extensionId, context, true)
        else vm.play(extensionId, context, true)
        return true
    }

    open fun openFeed(
        view: View?,
        extensionId: String?,
        feedId: String?,
        title: String?,
        subtitle: String?,
        feed: Feed<Shelf>?
    ): Boolean {
        val fragment = fragmentManager.findFragmentById(containerId)
            ?: return notFoundSnack(R.string.view)
        val vm by fragment.activityViewModels<FeedFragment.VM>()
        vm.extensionId = extensionId ?: return notFoundSnack(R.string.extension)
        vm.feedId = feedId ?: return notFoundSnack(R.string.item)
        vm.feed = feed ?: return notFoundSnack(R.string.feed)
        fragment.openFragment<FeedFragment>(view, FeedFragment.getBundle(title.orEmpty(), subtitle))
        afterOpen()
        return true
    }

    fun notFoundSnack(id: Int): Boolean = with(fragment) {
        val notFound = getString(R.string.no_x_found, getString(id))
        createSnack(notFound)
        false
    }

    open fun onMediaClicked(
        view: View?, extensionId: String?, item: EchoMediaItem?, context: EchoMediaItem?
    ): Boolean {
        if (extensionId == null) return notFoundSnack(R.string.extension)
        if (item == null) return notFoundSnack(R.string.item)
        return when (item) {
            is Track -> throw IllegalStateException()
            is Radio -> {
                val vm by fragment.activityViewModels<PlayerViewModel>()
                vm.play(extensionId, item, false)
                true
            }

            else -> {
                val fragment = fragmentManager.findFragmentById(containerId)
                    ?: return notFoundSnack(R.string.view)
                fragment.openFragment<MediaFragment>(view, getBundle(extensionId, item, false))
                afterOpen()
                true
            }
        }
    }

    open fun onMediaLongClicked(
        view: View?, extensionId: String?, item: EchoMediaItem?, context: EchoMediaItem?,
        tabId: String?, index: Int
    ): Boolean {
        if (extensionId == null) return notFoundSnack(R.string.extension)
        if (item == null) return notFoundSnack(R.string.item)
        MediaMoreBottomSheet.newInstance(
            containerId, extensionId, item, false,
            context = context, tabId = tabId, pos = index
        ).show(fragmentManager, null)
        return true
    }

    open fun onTracksClicked(
        view: View?,
        extensionId: String?,
        context: EchoMediaItem?,
        tracks: List<Track>?,
        pos: Int
    ): Boolean {
        if (extensionId == null) return notFoundSnack(R.string.extension)
        if (tracks.isNullOrEmpty()) return notFoundSnack(R.string.tracks)
        val vm by fragment.activityViewModels<PlayerViewModel>()
        vm.setQueue(extensionId, tracks, pos, context)
        vm.setPlaying(true)
        return true
    }

    open fun onTrackSwiped(
        view: View?, extensionId: String?, track: Track?,
    ): Boolean {
        if (extensionId == null) return notFoundSnack(R.string.extension)
        if (track == null) return notFoundSnack(R.string.track)
        val vm by fragment.activityViewModels<PlayerViewModel>()
        vm.addToNext(extensionId, track, false)
        return true
    }
}