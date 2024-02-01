package dev.brahmkshatriya.echo.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val offlineExtension: OfflineExtension
) : ViewModel() {

}