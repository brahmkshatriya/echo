package dev.brahmkshatriya.echo.data.models

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

data class MediaItemsContainer(
    val title: String,
    val flow: Flow<PagingData<MediaItem>>
)