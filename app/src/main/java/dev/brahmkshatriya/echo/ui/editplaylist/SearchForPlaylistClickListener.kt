package dev.brahmkshatriya.echo.ui.editplaylist

import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.adapter.ShelfClickListener

class SearchForPlaylistClickListener(
    fragmentManager: FragmentManager
) : ShelfClickListener(fragmentManager, R.id.playlistSearchContainer) {
    val viewModel by fragment.activityViewModels<SearchForPlaylistViewModel>()

    override fun onClick(clientId: String, item: EchoMediaItem, transitionView: View?) {
        when (item) {
            is EchoMediaItem.TrackItem -> viewModel.addTrack(item.track)
            else -> super.onClick(clientId, item, transitionView)
        }
    }

    override fun onClick(
        clientId: String, context: EchoMediaItem?, list: List<Track>, pos: Int, view: View
    ) {
        viewModel.addTrack(list[pos])
    }
}