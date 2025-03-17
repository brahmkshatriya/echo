package dev.brahmkshatriya.echo.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaViewModel(
    val extensionId: String,
    val item: EchoMediaItem,
    val loaded: Boolean,
    val loadOther: Boolean,
    app: App,
    extensionLoader: ExtensionLoader,
) : ViewModel() {


    val extensions = extensionLoader.extensions.music
    val throwFlow = app.throwFlow
    val messageFlow = app.messageFlow
    val context = app.context

    val itemFlow = MutableStateFlow(item)
    var isLoading = !loaded
    val extensionFlow = MutableStateFlow<Extension<*>?>(null)

    val loadingFlow = MutableSharedFlow<Boolean>()
    private suspend fun loadItem(force: Boolean = false) = runCatching {
        if (!isLoading && !force) return@runCatching item
        isLoading = true
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
        isLoading = false
        loadingFlow.emit(false)
        item.getOrThrow()
    }

    private fun createMessage(message: String) {
        viewModelScope.launch {
            messageFlow.emit(Message(message))
        }
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
        createMessage(
            context.getString(
                if (follow) R.string.following_x else R.string.unfollowing_x,
                artist.name
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
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
        createMessage(
            context.getString(
                if (hide) R.string.hiding_x else R.string.unhiding_x,
                track.title
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
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
        createMessage(
            context.getString(
                if (like) R.string.liking_x else R.string.unliking_x,
                track.title
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
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

    fun deletePlaylist(playlist: Playlist) {
        TODO("Not yet implemented")
    }


    val tracks = MutableStateFlow<PagingUtils.Data<Track>>(
        PagingUtils.Data(null, null, null)
    )

    private suspend fun loadTracks() {
        val extension = extensionFlow.value
        tracks.value = PagingUtils.Data(extension, null, null)
        val data = when (val item = itemFlow.value) {
            is EchoMediaItem.Lists.AlbumItem ->
                extension?.get<AlbumClient, PagedData<Track>> { loadTracks(item.album) }

            is EchoMediaItem.Lists.PlaylistItem ->
                extension?.get<PlaylistClient, PagedData<Track>> { loadTracks(item.playlist) }

            is EchoMediaItem.Lists.RadioItem ->
                extension?.get<RadioClient, PagedData<Track>> { loadTracks(item.radio) }

            else -> null
        }?.getOrElse {
            tracks.value = PagingUtils.Data(extension, null, PagingUtils.errorPagingData(it))
            return
        } ?: return
        data.toFlow(extension!!).collectWith(throwFlow) {
            tracks.value = PagingUtils.Data(extension, data, it)
        }
    }

    val feed = MutableStateFlow<PagingUtils.Data<Shelf>>(
        PagingUtils.Data(null, null, null)
    )

    private suspend fun loadShelves() {
        val extension = extensionFlow.value
        feed.value = PagingUtils.Data(extension, null, null)
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
            feed.value = PagingUtils.Data(extension, null, PagingUtils.errorPagingData(it))
            return
        } ?: return
        data.toFlow(extension!!).collectWith(throwFlow) {
            feed.value = PagingUtils.Data(extension, data, it)
        }
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            itemFlow.value = loadItem(force).getOrElse {
                throwFlow.emit(it)
                item
            }
            loadSavedState()
            if (loadOther) {
                launch { loadTracks() }
                launch { loadShelves() }
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