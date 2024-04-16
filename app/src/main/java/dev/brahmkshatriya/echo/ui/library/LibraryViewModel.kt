package dev.brahmkshatriya.echo.ui.library

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: ExtensionModule.ExtensionFlow,
    val userFlow: MutableSharedFlow<UserEntity?>
) : CatchingViewModel(throwableFlow) {

    var recyclerPosition = 0

    val loading = MutableSharedFlow<Boolean>()
    val libraryFeed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    val genres = MutableStateFlow<List<Genre>>(emptyList())
    var genre: Genre? = null
        set(newValue) {
            val oldValue = field
            field = newValue
            if (oldValue != newValue) refresh()
        }

    override fun onInitialize() {
        viewModelScope.launch {
            extensionFlow.collect {
                val client = it as? LibraryClient ?: return@collect
                loadGenres(client)
            }
        }
    }

    private suspend fun loadGenres(client: LibraryClient) {
        loading.emit(true)
        val list = tryWith { client.getLibraryGenres() } ?: emptyList()
        loading.emit(false)
        genre = list.firstOrNull()
        genres.value = list
    }

    private suspend fun loadFeed(client: LibraryClient) = tryWith {
        libraryFeed.value = null
        client.getLibraryFeed(genre).collectTo(libraryFeed)
    }

    fun refresh(reset: Boolean = false) {
        val client = extensionFlow.value as? LibraryClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (reset) {
                genre = null
                loadGenres(client)
            }
            loadFeed(client)
        }
    }
}