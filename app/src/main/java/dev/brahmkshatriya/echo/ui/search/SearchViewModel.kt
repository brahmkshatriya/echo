package dev.brahmkshatriya.echo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import dev.brahmkshatriya.echo.data.models.MediaItemsContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val offlineExtension: OfflineExtension
) : ViewModel() {

    private val _result: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val result = _result.asStateFlow()
    var query: String? = null

    fun search(query: String) {
        this.query = query
        viewModelScope.launch(Dispatchers.IO) {
            offlineExtension.search(query).collectLatest {
                _result.value = it
            }
        }
    }
}