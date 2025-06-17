package dev.brahmkshatriya.echo.ui.main.library

import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.ui.main.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val app: App,
    extensionLoader: ExtensionLoader,
) : FeedViewModel(app.throwFlow, extensionLoader) {

    override suspend fun getTabs(extension: Extension<*>) =
        extension.get<LibraryFeedClient, List<Tab>> {
            getLibraryTabs()
        }

    override suspend fun getFeed(extension: Extension<*>) =
        extension.get<LibraryFeedClient, Feed> {
            getLibraryFeed(tab)
        }

    val createPlaylistStateFlow = MutableStateFlow<State>(State.CreatePlaylist)
    fun createPlaylist(title: String, desc: String?) {
        val extension = current.value ?: return
        createPlaylistStateFlow.value = State.Creating
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = extension.get<PlaylistEditClient, Playlist>(app.throwFlow) {
                createPlaylist(title, desc)
            }
            createPlaylistStateFlow.value = State.PlaylistCreated(extension.id, playlist)
        }
    }

    init { init() }

    sealed class State {
        data object CreatePlaylist : State()
        data object Creating : State()
        data class PlaylistCreated(val extensionId: String, val playlist: Playlist?) : State()
    }
}