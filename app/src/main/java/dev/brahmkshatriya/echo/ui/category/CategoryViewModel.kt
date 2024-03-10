package dev.brahmkshatriya.echo.ui.category

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import kotlinx.coroutines.flow.Flow

class CategoryViewModel : ViewModel() {
    var title: String? = null
    var flow: Flow<PagingData<EchoMediaItem>>? = null
}