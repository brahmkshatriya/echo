package dev.brahmkshatriya.echo.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import dev.brahmkshatriya.echo.NavigationDirections
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.player.PlayerViewModel

class MediaItemClickListener(
    private val fragment: Fragment
) : ClickListener<Pair<View, EchoMediaItem>> {
    override fun onClick(item: Pair<View, EchoMediaItem>) {
        val view = item.first
        when (val mediaItem = item.second) {
            is EchoMediaItem.AlbumItem -> NavigationDirections.actionAlbum(
                albumWithCover = mediaItem.album
            ).let {
                val transitionName = mediaItem.album.uri.toString()
                view.transitionName = transitionName
                val extras = FragmentNavigatorExtras(view to transitionName)
                fragment.findNavController().navigate(it, extras)
            }

//            is EchoMediaItem.ArtistItem -> TODO()
//            is EchoMediaItem.PlaylistItem -> TODO()
            is EchoMediaItem.TrackItem -> {
                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
                playerViewModel.play(mediaItem.track)
            }

            else -> {}
        }

    }

    override fun onLongClick(item: Pair<View, EchoMediaItem>) {
        when (val mediaItem = item.second) {
//            is EchoMediaItem.AlbumItem -> TODO()
//            is EchoMediaItem.ArtistItem -> TODO()
//            is EchoMediaItem.PlaylistItem -> TODO()
            is EchoMediaItem.TrackItem -> {
                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
                playerViewModel.addToQueue(mediaItem.track)
            }

            else -> {}
        }
    }
}

