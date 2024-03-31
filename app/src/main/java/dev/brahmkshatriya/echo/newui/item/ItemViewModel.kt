package dev.brahmkshatriya.echo.newui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ItemViewModel : ViewModel() {

    val itemFlow = MutableStateFlow<EchoMediaItem?>(null)
    val relatedFeed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)

    private var initialized = false
    private var throwableFlow: MutableSharedFlow<Throwable>? = null

    fun loadItem(
        throwableFlow: MutableSharedFlow<Throwable>,
        client: ExtensionClient,
        item: EchoMediaItem
    ) {
        if (initialized) return
        initialized = true
        this.throwableFlow = throwableFlow
        viewModelScope.launch {
            val mediaItem = when (item) {
                is EchoMediaItem.Lists.AlbumItem -> getClient<AlbumClient>(client) {
                    load(item.album, ::loadAlbum, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.Lists.PlaylistItem -> getClient<PlaylistClient>(client) {
                    load(item.playlist, ::loadPlaylist, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.Profile.ArtistItem -> getClient<ArtistClient>(client) {
                    load(item.artist, ::loadArtist, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.Profile.UserItem -> getClient<UserClient>(client) {
                    load(item.user, ::loadUser, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.TrackItem -> getClient<TrackClient>(client) {
                    load(item.track, ::loadTrack, ::getMediaItems)?.toMediaItem()
                }
            }
            mediaItem?.let { itemFlow.value = it }
        }
    }

    private inline fun <reified T> getClient(
        client: ExtensionClient, block: T.() -> EchoMediaItem?
    ) = if (client is T) block(client) else null

    private suspend fun <T> load(
        item: T, loadItem: suspend (T) -> T, loadRelated: (T) -> PagedData<MediaItemsContainer>
    ): T? {
        val flow = throwableFlow ?: return null
        return tryWith(flow) {
            val loaded = loadItem(item)
            val related = tryWith(flow) { loadRelated(loaded) }
            related?.cachedIn(viewModelScope)?.catchWith(flow)?.collect(relatedFeed)
            loaded
        }
    }
}
