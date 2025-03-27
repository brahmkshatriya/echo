package dev.brahmkshatriya.echo.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackHideClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.collectWith
import dev.brahmkshatriya.echo.ui.common.PagingUtils.toFlow
import dev.brahmkshatriya.echo.ui.player.info.TrackInfoViewModel.Companion.getTrackShelves
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaViewModel(
    private val extensionId: String,
    private val item: EchoMediaItem,
    loaded: Boolean,
    private val loadOther: Boolean,
    app: App,
    extensionLoader: ExtensionLoader,
) : ViewModel() {


    val extensions = extensionLoader.extensions.music
    val throwFlow = app.throwFlow
    val messageFlow = app.messageFlow
    val context = app.context

    val itemFlow = MutableStateFlow(item)
    val extensionFlow = MutableStateFlow<Extension<*>?>(null)

    val loadingFlow = MutableStateFlow(!loaded)
    private suspend fun loadItem(force: Boolean = false) = runCatching {
        if (!loadingFlow.value && !force) return@runCatching item
        loadingFlow.emit(true)
        val extension = extensions.getExtensionOrThrow(extensionId)
        val item = when (item) {
            is EchoMediaItem.Lists.RadioItem -> Result.success(item)
            is EchoMediaItem.Lists.AlbumItem -> extension.get<AlbumClient, EchoMediaItem> {
                loadAlbum(item.album).toMediaItem()
            }

            is EchoMediaItem.Lists.PlaylistItem -> extension.get<PlaylistClient, EchoMediaItem> {
                loadPlaylist(item.playlist).toMediaItem()
            }

            is EchoMediaItem.Profile.ArtistItem -> extension.get<ArtistClient, EchoMediaItem> {
                loadArtist(item.artist).toMediaItem()
            }

            is EchoMediaItem.Profile.UserItem -> extension.get<UserClient, EchoMediaItem> {
                loadUser(item.user).toMediaItem()
            }

            is EchoMediaItem.TrackItem -> extension.get<TrackClient, EchoMediaItem> {
                loadTrack(item.track).toMediaItem()
            }
        }
        loadingFlow.emit(false)
        item.getOrThrow()
    }

    private suspend fun createMessage(message: String) {
        messageFlow.emit(Message(message))
    }

    suspend fun onShare(): String? = withContext(Dispatchers.IO) {
        createMessage(context.getString(R.string.sharing_x, itemFlow.value.title))
        extensionFlow.value?.get<ShareClient, String>(throwFlow) {
            onShare(itemFlow.value)
        }
    }

    val savedState = MutableStateFlow(false)
    private fun loadSavedState() {
        viewModelScope.launch(Dispatchers.IO) {
            savedState.value = extensionFlow.value?.get<SaveToLibraryClient, Boolean>(throwFlow) {
                isSavedToLibrary(itemFlow.value)
            } ?: false
        }
    }

    fun saveToLibrary(save: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            createMessage(
                context.getString(
                    if (save) R.string.saving_x_to_library else R.string.removing_x_from_library,
                    itemFlow.value.title
                )
            )
            extensionFlow.value?.get<SaveToLibraryClient, Unit>(throwFlow) {
                saveToLibrary(itemFlow.value, save)
            }
            createMessage(
                context.getString(
                    if (save) R.string.saved_x_to_library else R.string.removed_x_from_library,
                    itemFlow.value.title
                )
            )
            loadSavedState()
        }
    }

    fun follow(artist: Artist, follow: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            createMessage(
                context.getString(
                    if (follow) R.string.following_x else R.string.unfollowing_x,
                    artist.name
                )
            )
            extensionFlow.value?.get<ArtistFollowClient, Unit>(throwFlow) {
                followArtist(artist, follow)
            }
            createMessage(
                context.getString(
                    if (follow) R.string.followed_x else R.string.unfollowed_x,
                    artist.name
                )
            )
            loadItem(true)
        }
    }

    fun hide(track: Track, hide: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            createMessage(
                context.getString(
                    if (hide) R.string.hiding_x else R.string.unhiding_x,
                    track.title
                )
            )
            extensionFlow.value?.get<TrackHideClient, Unit>(throwFlow) {
                hideTrack(track, hide)
            }
            createMessage(
                context.getString(
                    if (hide) R.string.hid_x else R.string.unhid_x,
                    track.title
                )
            )
            loadItem(true)
        }
    }

    fun like(track: Track, like: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            createMessage(
                context.getString(
                    if (like) R.string.liking_x else R.string.unliking_x,
                    track.title
                )
            )
            extensionFlow.value?.get<TrackLikeClient, Unit>(throwFlow) {
                likeTrack(track, like)
            }
            createMessage(
                context.getString(
                    if (like) R.string.liked_x else R.string.unliked_x,
                    track.title
                )
            )
            loadItem(true)
        }
    }

    sealed class State {
        data object DeletePlaylist : State()
        data object Deleting : State()
        data class PlaylistDeleted(val extensionId: String, val playlist: Playlist?) : State()
    }

    val deleteFlow = MutableStateFlow<State>(State.DeletePlaylist)
    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteFlow.value = State.Deleting
            createMessage(context.getString(R.string.deleting_x, playlist.title))
            val success = extensionFlow.value?.get<PlaylistEditClient, Playlist>(throwFlow) {
                deletePlaylist(playlist)
                playlist
            }
            createMessage(context.getString(R.string.deleted_x, playlist.title))
            deleteFlow.value = State.PlaylistDeleted(extensionId, success)
        }
    }

    val tracks = MutableStateFlow<PagingUtils.Data<Track>>(
        PagingUtils.Data(null, null, null, null)
    )

    private suspend fun loadTracks() {
        val extension = extensionFlow.value
        tracks.value = PagingUtils.Data(extension, item.id, null, null)
        val data = when (val item = itemFlow.value) {
            is EchoMediaItem.Lists.AlbumItem ->
                extension?.get<AlbumClient, PagedData<Track>> { loadTracks(item.album) }

            is EchoMediaItem.Lists.PlaylistItem ->
                extension?.get<PlaylistClient, PagedData<Track>> { loadTracks(item.playlist) }

            is EchoMediaItem.Lists.RadioItem ->
                extension?.get<RadioClient, PagedData<Track>> { loadTracks(item.radio) }

            else -> null
        }?.getOrElse {
            tracks.value =
                PagingUtils.Data(extension, item.id, null, PagingUtils.errorPagingData(it))
            return
        } ?: return
        data.toFlow(extension!!).collectWith(throwFlow) {
            tracks.value = PagingUtils.Data(extension, item.id, data, it)
        }
    }

    val feed = MutableStateFlow<PagingUtils.Data<Shelf>>(
        PagingUtils.Data(null, item.id, null, null)
    )

    private suspend fun loadShelves() {
        val extension = extensionFlow.value
        feed.value = PagingUtils.Data(extension, item.id, null, null)
        val data = when (val item = itemFlow.value) {
            is EchoMediaItem.Profile.ArtistItem ->
                extension?.get<ArtistClient, PagedData<Shelf>> { getShelves(item.artist) }

            is EchoMediaItem.Profile.UserItem ->
                extension?.get<UserClient, PagedData<Shelf>> { getShelves(item.user) }

            is EchoMediaItem.Lists.AlbumItem ->
                extension?.get<AlbumClient, PagedData<Shelf>> { getShelves(item.album) }

            is EchoMediaItem.Lists.PlaylistItem ->
                extension?.get<PlaylistClient, PagedData<Shelf>> { getShelves(item.playlist) }

            is EchoMediaItem.TrackItem -> extension?.get<TrackClient, PagedData<Shelf>> {
                getTrackShelves(this, item.track)
            }

            else -> null
        }?.getOrElse {
            feed.value = PagingUtils.Data(extension, item.id, null, PagingUtils.errorPagingData(it))
            return
        } ?: return
        data.toFlow(extension!!).collectWith(throwFlow) {
            feed.value = PagingUtils.Data(extension, item.id, data, it)
        }
    }

    val trackJob = MutableStateFlow<Job?>(null)
    val shelfJob = MutableStateFlow<Job?>(null)
    fun refresh(force: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            itemFlow.value = loadItem(force).getOrElse {
                throwFlow.emit(it)
                item
            }
            loadSavedState()
            if (loadOther) {
                trackJob.value?.cancel()
                trackJob.value = launch { loadTracks() }
                shelfJob.value?.cancel()
                shelfJob.value = launch { loadShelves() }
            }
        }
    }


    init {
        viewModelScope.launch {
            extensions.collectLatest {
                extensionFlow.value = extensions.getExtension(extensionId)
            }
        }
        refresh(false)
    }
}