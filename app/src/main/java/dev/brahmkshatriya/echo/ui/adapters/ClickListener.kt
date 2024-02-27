package dev.brahmkshatriya.echo.ui.adapters

import androidx.navigation.NavController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.player.PlayerViewModel

class MediaItemListener(
    private val navController: NavController, private val playerViewModel: PlayerViewModel
) : ClickListener<EchoMediaItem> {
    override fun onClick(item: EchoMediaItem) {
        when (item) {
            is EchoMediaItem.AlbumItem -> {
//                val extras = FragmentNavigatorExtras(view to "shared_element_container")
                navController.navigate(R.id.fragment_album)
            }
//            is EchoMediaItem.ArtistItem -> TODO()
//            is EchoMediaItem.PlaylistItem -> TODO()
            is EchoMediaItem.TrackItem -> playerViewModel.play(item.track)
            else -> {}
        }

    }

    override fun onLongClick(item: EchoMediaItem) {
        when (item) {
//            is EchoMediaItem.AlbumItem -> TODO()
//            is EchoMediaItem.ArtistItem -> TODO()
//            is EchoMediaItem.PlaylistItem -> TODO()
            is EchoMediaItem.TrackItem -> playerViewModel.addToQueue(item.track)
            else -> {}
        }
    }
}

interface ClickListener<T> {
    fun onClick(item: T)
    fun onLongClick(item: T)
}