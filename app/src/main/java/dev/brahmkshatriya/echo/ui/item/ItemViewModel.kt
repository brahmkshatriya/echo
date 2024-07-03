package dev.brahmkshatriya.echo.ui.item

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Companion.deletePlaylist
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
    private val context: Application
) : CatchingViewModel(throwableFlow) {

    var item: EchoMediaItem? = null
    var loaded: EchoMediaItem? = null
    var client: ExtensionClient? = null
    var isRadioClient = false
    var isFollowClient = false

    val itemFlow = MutableStateFlow<EchoMediaItem?>(null)
    val relatedFeed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            loaded = null
            itemFlow.emit(null)
            val mediaItem = tryWith {
                when (val item = item!!) {
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
            }
            loaded = mediaItem
            itemFlow.emit(mediaItem)
        }
    }

    override fun onInitialize() {
        load()
    }

    private inline fun <reified T> getClient(
        client: ExtensionClient?, block: T.() -> EchoMediaItem?
    ) = if (client is T) block(client) else null

    private suspend fun <T> load(
        item: T, loadItem: suspend (T) -> T, loadRelated: (T) -> PagedData<MediaItemsContainer>
    ): T? {
        return tryWith {
            val loaded = loadItem(item)
            viewModelScope.launch {
                tryWith {
                    loadRelated(loaded).toFlow().map { it }
                }?.collectTo(relatedFeed)
            }
            loaded
        }
    }

    val shareLink = MutableSharedFlow<String>()
    fun onShare(client: ShareClient, item: EchoMediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val link = when (item) {
                is EchoMediaItem.Lists.AlbumItem -> client.onShare(item.album)
                is EchoMediaItem.Lists.PlaylistItem -> client.onShare(item.playlist)
                is EchoMediaItem.Profile.ArtistItem -> client.onShare(item.artist)
                is EchoMediaItem.Profile.UserItem -> client.onShare(item.user)
                is EchoMediaItem.TrackItem -> client.onShare(item.track)
            }
            shareLink.emit(link)
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) = viewModelScope.launch {
        deletePlaylist(extensionListFlow, mutableMessageFlow, context, clientId, playlist)
    }

    private val songsFlow = MutableStateFlow<PagingData<Track>?>(null)
    val songsLiveData: LiveData<PagingData<Track>?> = liveData {
        emitSource(songsFlow.asLiveData())
    }

    fun loadAlbum(album: Album) {
        val client = client
        if (client !is AlbumClient) return
        viewModelScope.launch(Dispatchers.IO) {
            songsFlow.value = null
            val tracks = tryWith { client.loadTracks(album) }
            tracks?.toFlow()?.collectTo(songsFlow)
        }
    }

    fun loadPlaylist(playlist: Playlist) {
        val client = client
        if (client !is PlaylistClient) return
        viewModelScope.launch(Dispatchers.IO) {
            songsFlow.value = null
            val tracks = tryWith { client.loadTracks(playlist) }
            tracks?.toFlow()?.collectTo(songsFlow)
        }
    }
}
