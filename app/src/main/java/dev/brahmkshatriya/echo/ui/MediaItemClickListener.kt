package dev.brahmkshatriya.echo.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dev.brahmkshatriya.echo.NavigationDirections
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.player.PlayerViewModel

class MediaItemClickListener(
    private val fragment: Fragment
) : ClickListener<EchoMediaItem> {
    override fun onClick(item: EchoMediaItem) {
        when (item) {
            is EchoMediaItem.AlbumItem -> NavigationDirections.actionAlbum(
                albumWithCover = item.album
            ).let {
                fragment.findNavController().navigate(it)
            }

//            is EchoMediaItem.ArtistItem -> TODO()
//            is EchoMediaItem.PlaylistItem -> TODO()
            is EchoMediaItem.TrackItem -> {
                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
                playerViewModel.play(item.track)
            }

            else -> {}
        }

    }

    override fun onLongClick(item: EchoMediaItem) {
        when (item) {
//            is EchoMediaItem.AlbumItem -> TODO()
//            is EchoMediaItem.ArtistItem -> TODO()
//            is EchoMediaItem.PlaylistItem -> TODO()
            is EchoMediaItem.TrackItem -> {
                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
                playerViewModel.play(item.track)
            }

            else -> {}
        }
    }
}

