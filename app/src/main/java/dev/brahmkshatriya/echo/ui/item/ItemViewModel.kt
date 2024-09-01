package dev.brahmkshatriya.echo.ui.item

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.plugger.ExtensionInfo
import dev.brahmkshatriya.echo.plugger.GenericExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
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
) : CatchingViewModel(throwableFlow) {

    var item: EchoMediaItem? = null
    var extension: GenericExtension? = null
    var isRadioClient = false
    var isFollowClient = false

    val itemFlow = MutableStateFlow<EchoMediaItem?>(null)
    val relatedFeed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            itemFlow.value = null
            val mediaItem = when (val item = item!!) {
                is EchoMediaItem.Lists.AlbumItem -> getClient<AlbumClient, EchoMediaItem> {
                    load(it, item.album, ::loadAlbum, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.Lists.PlaylistItem -> getClient<PlaylistClient, EchoMediaItem> {
                    load(it, item.playlist, ::loadPlaylist, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.Profile.ArtistItem -> getClient<ArtistClient, EchoMediaItem> {
                    load(it, item.artist, ::loadArtist, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.Profile.UserItem -> getClient<UserClient, EchoMediaItem> {
                    load(it, item.user, ::loadUser, ::getMediaItems)?.toMediaItem()
                }

                is EchoMediaItem.TrackItem -> getClient<TrackClient, EchoMediaItem> {
                    load(it, item.track, ::loadTrack, ::getMediaItems)?.toMediaItem()
                }
            }
            itemFlow.value = mediaItem
        }
    }

    override fun onInitialize() {
        load()
    }

    private inline fun <reified T, R> getClient(
        block: T.(info: ExtensionInfo) -> R?
    ) = extension?.run {
        val client = client
        if (client is T) block(client, info) else null
    }

    private suspend fun <T> load(
        info: ExtensionInfo,
        item: T,
        loadItem: suspend (T) -> T,
        loadRelated: (T) -> PagedData<MediaItemsContainer>
    ): T? {
        return tryWith(info) {
            val loaded = loadItem(item)
            viewModelScope.launch {
                tryWith(info) {
                    loadRelated(loaded).toFlow().map { it }
                }?.collectTo(relatedFeed)
            }
            loaded
        }
    }

    private val songsFlow = MutableStateFlow<PagingData<Track>?>(null)
    val songsLiveData = songsFlow.asLiveData()

    fun loadAlbumTracks(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<AlbumClient, Unit> {
                songsFlow.value = null
                val tracks = tryWith(it) { loadTracks(album) }
                tracks?.toFlow()?.collectTo(songsFlow)
            }
        }
    }

    fun loadPlaylistTracks(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<PlaylistClient, Unit> {
                songsFlow.value = null
                val tracks = tryWith(it) { loadTracks(playlist) }
                tracks?.toFlow()?.collectTo(songsFlow)
            }
        }
    }

    fun subscribe(artist: Artist, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<ArtistFollowClient, Unit> {
                tryWith(it) { if (subscribe) followArtist(artist) else unfollowArtist(artist) }
                load()
            }
        }
    }
}
