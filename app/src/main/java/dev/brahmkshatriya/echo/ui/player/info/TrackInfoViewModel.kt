package dev.brahmkshatriya.echo.ui.player.info

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.run
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.PagingUtils.collectWith
import dev.brahmkshatriya.echo.ui.common.PagingUtils.toFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TrackInfoViewModel(
    val app: App,
    extensionLoader: ExtensionLoader,
    playerState: PlayerState,
) : ViewModel() {

    private val extensions = extensionLoader.extensions
    val currentFlow = playerState.current
    private var previous: Track? = null
    val itemsFlow = MutableStateFlow<PagingData<Shelf>?>(null)

    fun load() {
        if (previous?.id == currentFlow.value?.mediaItem?.track?.id) return
        val current = currentFlow.value?.mediaItem ?: return
        if (!current.isLoaded) return
        viewModelScope.launch {
            val extension = extensions.music.getExtension(current.extensionId) ?: return@launch
            itemsFlow.value = null
            previous = current.track
            val shelves = extension.run(app.throwFlow) {
                app.context.getTrackShelves(this, current.track)
            } ?: return@launch
            shelves.toFlow().collectWith(app.throwFlow, itemsFlow)
        }
    }

    companion object {
        fun Context.getTrackShelves(
            ext: Any, track: Track
        ): PagedData.Concat<Shelf> {
            val album = track.album
            val artists = track.artists
            return PagedData.Concat(
                if (album != null) PagedData.Single {
                    val a = if (ext !is AlbumClient) album
                    else ext.loadAlbum(album)
                    listOf(a.toMediaItem().toShelf())
                } else PagedData.empty(),
                if (artists.isNotEmpty()) PagedData.Single {
                    listOf(
                        Shelf.Lists.Items(
                            getString(R.string.artists),
                            if (ext is ArtistClient) artists.map {
                                val artist = ext.loadArtist(it)
                                artist.toMediaItem()
                            } else artists.map { it.toMediaItem() }
                        )
                    )
                } else PagedData.empty(),
                if (ext is TrackClient) ext.getShelves(track) else PagedData.empty()
            )
        }
    }
}