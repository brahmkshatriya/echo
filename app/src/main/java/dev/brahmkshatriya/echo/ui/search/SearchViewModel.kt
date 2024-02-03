package dev.brahmkshatriya.echo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val offlineExtension: OfflineExtension
) : ViewModel() {
    suspend fun search(query: String) = offlineExtension.search(query).cachedIn(viewModelScope)
}