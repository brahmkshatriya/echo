package dev.brahmkshatriya.echo.ui.editplaylist

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.AlbumItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.PlaylistItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Companion.addToPlaylists
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val messageFlow: MutableSharedFlow<Message>,
    private val app: Application,
) : CatchingViewModel(throwableFlow) {
    val playlists = MutableStateFlow<List<Playlist>?>(null)
    val selectedPlaylists = mutableListOf<Playlist>()
    lateinit var clientId: String
    lateinit var item: EchoMediaItem
    val tracks = mutableListOf<Track>()
    override fun onInitialize() {
        val extension = extensionListFlow.getExtension(clientId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val mediaItem = item) {
                is AlbumItem -> extension.get<AlbumClient, Unit>(throwableFlow) {
                    tracks.addAll(loadTracks(mediaItem.album).loadAll())
                }

                is PlaylistItem -> extension.get<PlaylistClient, Unit>(throwableFlow) {
                    tracks.addAll(loadTracks(mediaItem.playlist).loadAll())
                }

                is TrackItem -> tracks.add(mediaItem.track)
                else -> throw IllegalStateException()
            }
            playlists.value = extension.get<PlaylistEditClient, List<Playlist>>(throwableFlow) {
                listEditablePlaylists()
            }
            if (playlists.value == null) dismiss.emit(Unit)
        }
    }

    fun togglePlaylist(playlist: Playlist) = with(selectedPlaylists) {
        if (contains(playlist)) remove(playlist) else add(playlist)
    }

    var saving = false
    val dismiss = MutableSharedFlow<Unit>()
    fun addToPlaylists() = viewModelScope.launch {
        saving = true
        addToPlaylists(
            extensionListFlow, messageFlow, app, clientId, selectedPlaylists, tracks
        )
        saving = false
        dismiss.emit(Unit)
    }

    fun reload() {
        val extension = extensionListFlow.getExtension(clientId) ?: return
        playlists.value = null
        viewModelScope.launch(Dispatchers.IO) {
            playlists.value = extension.get<PlaylistEditClient, List<Playlist>>(throwableFlow) {
                listEditablePlaylists()
            }
            if (playlists.value == null) dismiss.emit(Unit)
        }
    }
}
