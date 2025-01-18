package dev.brahmkshatriya.echo.ui.shelf

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.run
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelfSearchViewModel @Inject constructor(
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    var actionPerformed = false
    var clientId: String? = null
    var shelves: PagedData<Shelf>? = null
    private var _data: List<Shelf>? = null
    val flow = MutableStateFlow<List<Shelf>?>(null)

    var sorts = listOf<Sort>()

    override fun onInitialize() {
        viewModelScope.launch(Dispatchers.IO) {
            val shelf = shelves!!
            val list = mutableListOf<Shelf>()
            extensionListFlow.getExtension(clientId)?.run(throwableFlow) {
                val (l, c) = shelf.loadList(null)
                list.addAll(l)
                var cont = c
                var count = 1
                while (cont != null && count < MAX) {
                    val page = shelf.loadList(cont)
                    list.addAll(page.data)
                    cont = page.continuation
                    count++
                }
            }
            val data = list.flatMap { item ->
                when (item) {
                    is Shelf.Category -> listOf(item)
                    is Shelf.Item -> listOf(item.copy(loadTracks = false))
                    is Shelf.Lists.Categories -> item.list
                    is Shelf.Lists.Items -> item.list.map { it.toShelf() }
                    is Shelf.Lists.Tracks -> item.list.map { it.toMediaItem().toShelf() }
                }
            }
            sorts = Sort.getSorts(data)
            _data = data
            flow.value = data
        }
    }

    var query = ""
    var sort: Sort? = null
    var descending = false
    fun search(query: String) {
        this.query = query
        viewModelScope.launch(Dispatchers.IO) {
            val data = _data ?: return@launch

            if (query.isBlank()) {
                flow.value = applySort(data, sort, descending)
                return@launch
            }
            flow.value = null
            val list = data.searchBy(query) {
                when (it) {
                    is Shelf.Category -> listOf(it.title, it.subtitle)
                    is Shelf.Item -> listOf(it.media.title, it.media.subtitle)
                    is Shelf.Lists<*> -> error("???")
                }
            }.map { it.second }
            flow.value = applySort(list, sort, descending)
        }
    }

    fun sort(sort: Sort?, descending: Boolean) {
        this.sort = sort
        this.descending = descending
        viewModelScope.launch(Dispatchers.IO) {
            val data = _data ?: return@launch
            val sorted = applySort(data, sort, descending)
            flow.value = sorted
        }
    }

    private fun applySort(data: List<Shelf>, sort: Sort?, descending: Boolean): List<Shelf> {
        return sort?.sorter?.let {
            val new = it(data)
            if (descending) new.reversed() else new
        } ?: data
    }

    companion object {
        const val MAX = 10
    }
}