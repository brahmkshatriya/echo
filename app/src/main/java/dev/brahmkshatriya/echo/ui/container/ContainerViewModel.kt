package dev.brahmkshatriya.echo.ui.container

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainerViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {
    var moreFlow: Flow<PagingData<MediaItemsContainer>>? = null
    val flow = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    override fun onInitialize() {
        viewModelScope.launch {
            moreFlow?.collectTo { data ->
                flow.value = data.map { it }
            }
        }
    }
}