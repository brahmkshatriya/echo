package dev.brahmkshatriya.echo.ui.library

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.ui.common.FeedViewModel
import dev.brahmkshatriya.echo.ui.paging.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    override val extensionFlow: MutableStateFlow<MusicExtension?>,
    override val userFlow: MutableSharedFlow<UserEntity?>,
    override val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : FeedViewModel(throwableFlow, userFlow, extensionFlow, extensionListFlow) {
    override suspend fun getTabs(client: ExtensionClient) =
        (client as? LibraryClient)?.getLibraryTabs()

    override fun getFeed(client: ExtensionClient) =
        (client as? LibraryClient)?.getLibraryFeed(tab)?.toFlow()

    val playlistCreatedFlow = MutableSharedFlow<Pair<String, Playlist>>()
    fun createPlaylist(title: String) {
        val extension = extensionFlow.value ?: return
        val client = extension.client as? PlaylistEditClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = tryWith(extension.info) { client.createPlaylist(title, null) }
                ?: return@launch
            playlistCreatedFlow.emit(extension.metadata.id to playlist)
        }
    }
}