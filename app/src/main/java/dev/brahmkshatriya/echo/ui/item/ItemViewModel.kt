package dev.brahmkshatriya.echo.ui.item

import android.app.Application
import android.content.Context
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
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.run
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val app: Application,
    private val snackBar: MutableSharedFlow<Message>
) : CatchingViewModel(throwableFlow) {

    var item: EchoMediaItem? = null
    var extension: Extension<*>? = null
    var isRadioClient = false
    var isFollowClient = false

    val itemFlow = MutableStateFlow<EchoMediaItem?>(null)
    var loadRelatedFeed = true
    var relatedFeed: PagedData<Shelf>? = null
    val relatedFeedFlow = MutableStateFlow<PagingData<Shelf>?>(null)

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            itemFlow.value = null
            tracks = null
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
                    item, { loadTrack(it.track).toMediaItem() }, { getTrackShelves(it.track, app) }
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
                if (this !is SaveToLibraryClient) return@run
                savedState.value = isSavedToLibrary(loaded)
            }

            viewModelScope.launch {
                if (!loadRelatedFeed) return@launch
                relatedFeed = extension?.run(throwableFlow) { loadRelated(loaded) }
                relatedFeed?.toFlow()?.collectTo(relatedFeedFlow)
            }
            loaded
        }
    }

    override fun onInitialize() {
        load()
    }

    private suspend inline fun <reified T, reified R : Any> getClient(
        crossinline block: suspend T.() -> R
    ) = extension?.get<T, R>(throwableFlow, block)

    private val songsFlow = MutableStateFlow<PagingData<Track>?>(null)
    val songsLiveData = songsFlow.asLiveData()

    var tracks: PagedData<Track>? = null
    fun loadAlbumTracks(album: Album) {
        if (tracks == null) viewModelScope.launch(Dispatchers.IO) {
            getClient<AlbumClient, Unit> {
                songsFlow.value = null
                tracks = loadTracks(album)
                tracks?.toFlow()?.collectTo(songsFlow)
            }
        }
    }

    fun loadPlaylistTracks(playlist: Playlist) {
        if (tracks == null) viewModelScope.launch(Dispatchers.IO) {
            getClient<PlaylistClient, Unit> {
                songsFlow.value = null
                tracks = loadTracks(playlist)
                tracks?.toFlow()?.collectTo(songsFlow)
            }
        }
    }

    fun loadRadioTracks(radio: Radio) {
        if (tracks == null) viewModelScope.launch(Dispatchers.IO) {
            getClient<RadioClient, Unit> {
                songsFlow.value = null
                tracks = loadTracks(radio)
                tracks?.toFlow()?.collectTo(songsFlow)
            }
        }
    }

    private fun createSnack(message: String) =
        viewModelScope.launch { snackBar.emit(Message(message)) }

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

    fun subscribe(artist: Artist, subscribe: Boolean, reload: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<ArtistFollowClient, Unit> {
                followArtist(artist, subscribe)
                val message = if (subscribe) app.getString(R.string.following_artist, artist.name)
                else app.getString(R.string.unfollowed_artist, artist.name)
                createSnack(message)
            }
            if (reload) load()
        }
    }

    fun saveToLibrary(item: EchoMediaItem, save: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            getClient<SaveToLibraryClient, Unit> {
                saveToLibrary(item, save)
                if (save)
                    createSnack(app.getString(R.string.saved_item_to_library, item.title))
                else
                    createSnack(app.getString(R.string.removed_item_from_library, item.title))
            }
        }
    }

    companion object {

        fun Any.getTrackShelves(
            track: Track, context: Context
        ): PagedData.Concat<Shelf> {
            val album = track.album
            val artists = track.artists
            return PagedData.Concat(
                if (album != null) PagedData.Single {
                    val a = if (this !is AlbumClient) album
                    else loadAlbum(album)
                    listOf(a.toMediaItem().toShelf())
                } else PagedData.empty(),
                if (artists.isNotEmpty()) PagedData.Single {
                    listOf(
                        Shelf.Lists.Items(
                            context.getString(R.string.artists),
                            if (this is ArtistClient) artists.map {
                                val artist = loadArtist(it)
                                artist.toMediaItem()
                            } else artists.map { it.toMediaItem() }
                        )
                    )
                } else PagedData.empty(),
                if (this is TrackClient) getShelves(track) else PagedData.empty()
            )
        }
    }
}
