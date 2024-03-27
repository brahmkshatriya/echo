package dev.brahmkshatriya.echo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val homeFeedFlow: ExtensionModule.ExtensionFlow,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : ViewModel() {

    private val _feed: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val feed = _feed.asStateFlow()

    private var genre: MutableStateFlow<Genre?> = MutableStateFlow(null)
    var genres: MutableStateFlow<List<Genre>?> = MutableStateFlow(null)
    private var homeClient: HomeFeedClient? = null


    init {
        viewModelScope.launch {
            observe(homeFeedFlow.flow) {
                homeClient = it as? HomeFeedClient
                genre.value = null
                launch(Dispatchers.IO) {
                    tryWith(throwableFlow) {
                        genres.value = homeClient?.getHomeGenres()
                        genre.value = genres.value?.firstOrNull()
                    }
                    tryWith(throwableFlow) {
                        homeClient?.getHomeFeed(genre.value)
                            ?.cachedIn(viewModelScope)
                            ?.catchWith(throwableFlow)
                            ?.collectLatest { feed ->
                                _feed.value = feed
                            }
                    }
                }
            }
        }
    }

    fun loadGenres() {
        viewModelScope.launch(Dispatchers.IO) {
            genre.value = null
            tryWith(throwableFlow) {
                genres.value = homeClient?.getHomeGenres()
            }
        }
    }

    fun setGenre(it: Genre) {
        genre.value = it
    }

    fun getGenres(): List<Pair<Boolean, Genre>> {
        return genres.value?.map { Pair(it == genre.value, it) } ?: emptyList()
    }
}