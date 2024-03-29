package dev.brahmkshatriya.echo.ui

import android.view.View
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

class MediaItemClickListener(
    val fragment: Fragment
) : ClickListener<Pair<View, MediaItemsContainer>> {
    override fun onClick(item: Pair<View, MediaItemsContainer>) {
        val view = item.first
        when (val mediaItem = item.second) {
//            is MediaItemsContainer.AlbumItem -> {
//                val action = NavigationDirections.actionAlbum(mediaItem.album)
//                val extras = FragmentNavigatorExtras(view to view.transitionName)
//                fragment.findNavController().navigate(action, extras)
//            }
//
//            is MediaItemsContainer.ArtistItem -> {
//                val action = NavigationDirections.actionArtist(mediaItem.artist)
//                val extras = FragmentNavigatorExtras(view to view.transitionName)
//                fragment.findNavController().navigate(action, extras)
//            }
//
//            is MediaItemsContainer.TrackItem -> {
//                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
////                animatePlayerImage(fragment, view)
//
//                playerViewModel.play(mediaItem.track)
//            }
//
//            is MediaItemsContainer.Category -> {
//                val flow = mediaItem.more ?: return
//                val categoryViewModel: CategoryViewModel by fragment.activityViewModels()
//                categoryViewModel.title = mediaItem.title
//                categoryViewModel.flow = flow
//                val action = NavigationDirections.actionCategory()
//                val extras = FragmentNavigatorExtras(view to view.transitionName)
//                fragment.findNavController().navigate(action, extras)
//            }
//
//            is MediaItemsContainer.PlaylistItem -> {
//                val action = NavigationDirections.actionPlaylist(mediaItem.playlist)
//                val extras = FragmentNavigatorExtras(view to view.transitionName)
//                fragment.findNavController().navigate(action, extras)
//            }

            else -> {}
        }

    }

    override fun onLongClick(item: Pair<View, MediaItemsContainer>) {
        when (val mediaItem = item.second) {
//            is MediaItemsContainer.TrackItem -> {
//                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
//                playerViewModel.addToQueue(mediaItem.track)
//            }

            else -> {}
        }
    }
}

