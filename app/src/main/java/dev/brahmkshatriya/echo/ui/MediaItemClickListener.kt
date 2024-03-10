package dev.brahmkshatriya.echo.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.NavigationDirections
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.category.CategoryViewModel

class MediaItemClickListener(
    val fragment: Fragment
) : ClickListener<Pair<View, MediaItemsContainer>> {
    override fun onClick(item: Pair<View, MediaItemsContainer>) {
        val view = item.first
        when (val mediaItem = item.second) {
            is MediaItemsContainer.AlbumItem -> {
                val action = NavigationDirections.actionAlbum(albumWithCover = mediaItem.album)
                val extras = FragmentNavigatorExtras(view to view.transitionName)
                fragment.findNavController().navigate(action, extras)
            }

            is MediaItemsContainer.ArtistItem -> {
                val action = NavigationDirections.actionArtist(artistWithCover = mediaItem.artist)
                val extras = FragmentNavigatorExtras(view to view.transitionName)
                fragment.findNavController().navigate(action, extras)
            }
            is MediaItemsContainer.TrackItem -> {
                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
//                animatePlayerImage(fragment, view)
                playerViewModel.play(mediaItem.track)
            }
            is MediaItemsContainer.Category -> {
                val categoryViewModel : CategoryViewModel by fragment.activityViewModels()
                categoryViewModel.title = mediaItem.title
                categoryViewModel.flow = mediaItem.flow
                mediaItem.flow?.cachedIn(categoryViewModel.viewModelScope)
                val action = NavigationDirections.actionCategory()
                fragment.findNavController().navigate(action)
            }


            else -> {}
        }

    }

    override fun onLongClick(item: Pair<View, MediaItemsContainer>) {
        when (val mediaItem = item.second) {
            is MediaItemsContainer.TrackItem -> {
                val playerViewModel: PlayerViewModel by fragment.activityViewModels()
                playerViewModel.addToQueue(mediaItem.track)
            }

            else -> {}
        }
    }
}

