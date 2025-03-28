package dev.brahmkshatriya.echo.ui.playlist.edit.search

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener

class EditPlaylistSearchClickListener(
    private val manager: FragmentManager
) : ShelfClickListener(manager, R.id.playlistSearchContainer) {

    private val parentFragment: Fragment get() = manager.fragments.first().requireParentFragment()
    private val viewModel by lazy {
        parentFragment.viewModels<EditPlaylistSearchViewModel>().value
    }

    override fun onTrackClicked(
        extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
    ) {
        viewModel.addTrack(list[index])
    }

    override fun onMediaItemClicked(extensionId: String?, item: EchoMediaItem?, it: View?) {
        when (item) {
            is EchoMediaItem.TrackItem -> viewModel.addTrack(item.track)
            else -> super.onMediaItemClicked(extensionId, item, it)
        }
    }
}