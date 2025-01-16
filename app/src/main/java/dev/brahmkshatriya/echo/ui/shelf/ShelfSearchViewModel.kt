package dev.brahmkshatriya.echo.ui.shelf

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
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
    var searchBarClicked = false
    var shelves: PagedData<Shelf>? = null
    var data: List<Shelf>? = null
    val flow = MutableStateFlow<List<Shelf>?>(null)
    override fun onInitialize() {
        viewModelScope.launch(Dispatchers.IO) {
            val shelf = shelves!!
            val list = mutableListOf<Shelf>()
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
            data = list
            flow.value = list
        }
    }

    var query = ""
    fun search(query: String) {
        this.query = query
        viewModelScope.launch(Dispatchers.IO) {
            val data = data ?: return@launch
            if (query.isBlank()) {
                flow.value = data
                return@launch
            }
            flow.value = null
            val list = data.searchBy(query) {
                when (it) {
                    is Shelf.Category -> listOf(it.title, it.subtitle)
                    is Shelf.Item -> listOf(it.media.title, it.media.subtitle)
                    is Shelf.Lists<*> -> when (it) {
                        is Shelf.Lists.Categories -> it.list.flatMap { category ->
                            listOf(category.title, category.subtitle)
                        }

                        is Shelf.Lists.Items -> it.list.flatMap { item ->
                            listOf(item.title, item.subtitle)
                        }

                        is Shelf.Lists.Tracks -> it.list.flatMap { track ->
                            listOf(track.title, track.subtitle)
                        }
                    } + listOf(it.title, it.subtitle)
                }
            }.map { it.second }
            flow.value = list
        }
    }

    companion object {
        const val MAX = 5
    }
}