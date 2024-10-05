package dev.brahmkshatriya.echo.ui.item

import android.app.Application
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackHideClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.AlbumItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.PlaylistItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.RadioItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile.ArtistItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile.UserItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.run
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
    private val app: Application,
    private val snackBar: MutableSharedFlow<SnackBar.Message>
) : CatchingViewModel(throwableFlow) {

    var item: EchoMediaItem? = null
    var extension: Extension<*>? = null
    var isRadioClient = false
    var isFollowClient = false

    val itemFlow = MutableStateFlow<EchoMediaItem?>(null)
    var loadRelatedFeed = true
    val relatedFeed = MutableStateFlow<PagingData<Shelf>?>(null)

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            itemFlow.value = null
            val mediaItem = when (val item = item!!) {
                is AlbumItem -> loadItem<AlbumClient, AlbumItem>(
                    item, { loadAlbum(it.album).toMediaItem() }, { getShelves(it.album) }
                )

                is PlaylistItem -> loadItem<PlaylistClient, PlaylistItem>(
                    item,
                    { loadPlaylist(it.playlist).toMediaItem() },
                    { getShelves(it.playlist) }
                )

                is ArtistItem -> loadItem<ArtistClient, ArtistItem>(
                    item, { loadArtist(it.artist).toMediaItem() }, { getShelves(it.artist) }
                )

                is UserItem -> loadItem<UserClient, UserItem>(
                    item, { loadUser(it.user).toMediaItem() }, { getShelves(it.user) }
                )

                is TrackItem -> loadItem<TrackClient, TrackItem>(
                    item, { loadTrack(it.track).toMediaItem() }, { trackItem ->
                        val client = this
                        val track = trackItem.track
                        val album = trackItem.track.album
                        val artists = trackItem.track.artists
                        PagedData.Concat(
                            if (client is AlbumClient && album != null) PagedData.Single {
                                listOf(
                                    client.loadAlbum(album).toMediaItem().toShelf()
                                )
                            } else PagedData.empty(),
                            if (artists.isNotEmpty()) PagedData.Single {
                                listOf(
                                    Shelf.Lists.Items(
                                        app.getString(R.string.artists),
                                        if (client is ArtistClient) artists.map {
                                            val artist = client.loadArtist(it)
                                            artist.toMediaItem()
                                        } else artists.map { it.toMediaItem() }
                                    )
                                )
                            } else PagedData.empty(),
                            client.getShelves(track)
                        )
                    }
                )

                is RadioItem -> loadItem<RadioClient, RadioItem>(item, { it }, { null })
            }
            itemFlow.value = mediaItem
        }
    }

    val savedState = MutableStateFlow(false)
    private suspend inline fun <reified U, reified T : EchoMediaItem> loadItem(
        item: T,
        crossinline loadItem: suspend U.(T) -> T,
        crossinline loadRelated: U.(T) -> PagedData<Shelf>? = { null }
    ): T? {
        return getClient<U, T> {
            val loaded = loadItem(item)
            extension?.run(throwableFlow) {
                if (this is SaveToLibraryClient)
                    savedState.value = isSavedToLibrary(loaded)
            }

            viewModelScope.launch {
                if (loadRelatedFeed) extension?.run(throwableFlow) {
                    loadRelated(loaded)?.toFlow()?.map { it }
                }?.collectTo(relatedFeed)
            }
            loaded
        }
    }

    override fun onInitialize() {
        load()
    }

    private suspend inline fun <reified T, reified R : Any> getClient(
        block: T.() -> R
    ) = extension?.get<T, R>(throwableFlow, block)

    private val songsFlow = MutableStateFlow<PagingData<Track>?>(null)
    val songsLiveData = songsFlow.asLiveData()

    fun loadAlbumTracks(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<AlbumClient, Unit> {
                songsFlow.value = null
                val tracks = loadTracks(album)
                tracks.toFlow().collectTo(songsFlow)
            }
        }
    }

    fun loadPlaylistTracks(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<PlaylistClient, Unit> {
                songsFlow.value = null
                val tracks = loadTracks(playlist)
                tracks.toFlow().collectTo(songsFlow)
            }
        }
    }

    fun loadRadioTracks(radio: Radio) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<RadioClient, Unit> {
                songsFlow.value = null
                val tracks = loadTracks(radio)
                tracks.toFlow().collectTo(songsFlow)
            }
        }
    }

    private fun createSnack(message: String) =
        viewModelScope.launch { snackBar.emit(SnackBar.Message(message)) }

    fun like(track: Track, isLiked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<TrackLikeClient, Unit> {
                likeTrack(track, isLiked)
                val message = if (isLiked) app.getString(R.string.liked_track, track.title)
                else app.getString(R.string.unliked_track, track.title)
                createSnack(message)
            }
        }
    }


    fun hide(track: Track, isHidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<TrackHideClient, Unit> {
                hideTrack(track, isHidden)
                val message = if (isHidden) app.getString(R.string.hidden_track, track.title)
                else app.getString(R.string.unhidden_track, track.title)
                createSnack(message)
            }
        }
    }

    fun subscribe(artist: Artist, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<ArtistFollowClient, Unit> {
                if (subscribe) followArtist(artist) else unfollowArtist(artist)
                val message = if (subscribe) app.getString(R.string.following_artist, artist.name)
                else app.getString(R.string.unfollowed_artist, artist.name)
                createSnack(message)
            }
        }
    }

    fun saveToLibrary(item: EchoMediaItem, save: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<SaveToLibraryClient, Unit> {
                if (save) {
                    saveToLibrary(item)
                    createSnack(app.getString(R.string.saved_item_to_library, item.title))
                } else {
                    removeFromLibrary(item)
                    createSnack(app.getString(R.string.removed_item_from_library, item.title))
                }
            }
        }
    }

}
