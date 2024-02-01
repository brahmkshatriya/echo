package dev.brahmkshatriya.echo.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val offlineExtension: OfflineExtension
): ViewModel() {

}