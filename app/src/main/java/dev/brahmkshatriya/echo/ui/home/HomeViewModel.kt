package dev.brahmkshatriya.echo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.clients.HomeFeedClient
import dev.brahmkshatriya.echo.data.models.MediaItemsContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeClient: HomeFeedClient
) : ViewModel() {

    private val _feed: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val feed = _feed.asStateFlow()

    val genre: String? = null

    fun loadFeed(genre: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            println("Loading Data")
            homeClient.getHomeFeed(genre).cachedIn(viewModelScope).collectLatest {
                println("Data Received")
                _feed.value = it
            }
        }
    }
}