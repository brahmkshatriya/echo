package dev.brahmkshatriya.echo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.di.ExtensionFlow
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    val searchFlow: ExtensionFlow,
    private val throwableFlow: MutableSharedFlow<Throwable>,
) : ViewModel() {

    init {
        viewModelScope.launch {
            searchFlow.flow.collectLatest {
                searchClient = it as? SearchClient
            }
        }
    }

    private val _result: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val result = _result.asStateFlow()
    var query: String? = null

    private var searchClient: SearchClient? = null

    fun search(query: String) {
        this.query = query
        viewModelScope.launch(Dispatchers.IO) {
            tryWith(throwableFlow) {
                searchClient?.search(query)?.catchWith(throwableFlow)?.collectLatest {
                    _result.value = it
                }
            }
        }
    }
}