package dev.brahmkshatriya.echo.ui.player.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.collectWith
import dev.brahmkshatriya.echo.ui.common.PagingUtils.toFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TrackInfoViewModel(
    val app: App,
    extensionLoader: ExtensionLoader,
    playerState: PlayerState,
) : ViewModel() {

    private val extensions = extensionLoader
    val currentFlow = playerState.current
    private var previous: Track? = null
    val itemsFlow = MutableStateFlow<PagingUtils.Data<Shelf>>(
        PagingUtils.Data(null, null, null, null)
    )

    private var job: Job? = null
    fun load() {
        if (previous?.id == currentFlow.value?.mediaItem?.track?.id) return
        val current = currentFlow.value?.mediaItem ?: return
        job?.cancel()
        itemsFlow.value = PagingUtils.Data(null, null, null, null)
        if (!current.isLoaded) return
        val id = current.track.id
        job = viewModelScope.launch {
            val extension = extensions.music.getExtension(current.extensionId)
            itemsFlow.value = PagingUtils.Data(extension, id, null, null)
            previous = current.track
            val shelves = extension?.get<TrackClient, PagedData<Shelf>> {
                getTrackShelves(this, current.track)
            }?.getOrElse {
                itemsFlow.value =
                    PagingUtils.Data(extension, id, null, PagingUtils.errorPagingData(it))
                return@launch
            } ?: return@launch
            shelves.toFlow(extension).collectWith(app.throwFlow) {
                itemsFlow.value = PagingUtils.Data(extension, id, shelves, it)
            }
        }
    }

    companion object {
        fun getTrackShelves(
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
                *artists.map {
                    PagedData.Single<Shelf> {
                        listOf(
                            if (ext is ArtistClient) {
                                val artist = ext.loadArtist(it)
                                artist.toMediaItem().toShelf()
                            } else it.toMediaItem().toShelf()
                        )
                    }
                }.toTypedArray(),
                if (ext is TrackClient) ext.getShelves(track) else PagedData.empty()
            )
        }
    }
}