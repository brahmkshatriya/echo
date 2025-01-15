package dev.brahmkshatriya.echo.ui.shelf

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelfViewModel @Inject constructor(
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {
    var shelves: PagedData<Shelf>? = null
    val flow = MutableStateFlow<PagingData<Shelf>?>(null)
    override fun onInitialize() {
        viewModelScope.launch {
            shelves!!.toFlow().collectTo(flow)
        }
    }
}