package dev.brahmkshatriya.echo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
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

    init {
        viewModelScope.launch {
            loadFeed()
        }
    }

    private val _feed: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val feed = _feed.asStateFlow()

    val genre: String? = null

    fun loadFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            homeClient.getHomeFeed(genre).cachedIn(viewModelScope).collectLatest {
                _feed.value = it
            }
        }
    }
}