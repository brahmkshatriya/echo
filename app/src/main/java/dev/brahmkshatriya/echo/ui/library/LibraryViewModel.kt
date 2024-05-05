package dev.brahmkshatriya.echo.ui.library

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.ui.common.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    override val extensionFlow: ExtensionModule.ExtensionFlow,
    val userFlow: MutableSharedFlow<UserEntity?>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : FeedViewModel<LibraryClient>(throwableFlow, extensionFlow) {
    override suspend fun getTabs(client: LibraryClient) = client.getLibraryTabs()
    override fun getFeed(client: LibraryClient) = client.getLibraryFeed(tab)

    val playlistCreatedFlow = MutableSharedFlow<Pair<String, Playlist>>()
    fun createPlaylist(title: String) {
        val client = extensionFlow.value
        if (client !is LibraryClient) return
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = tryWith { client.createPlaylist(title, null) }
                ?: return@launch
            playlistCreatedFlow.emit(client.metadata.id to playlist)
        }
    }
}