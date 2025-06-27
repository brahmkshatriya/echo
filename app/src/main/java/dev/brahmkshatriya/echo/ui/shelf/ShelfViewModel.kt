package dev.brahmkshatriya.echo.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.collectWith
import dev.brahmkshatriya.echo.ui.common.PagingUtils.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ShelfViewModel(
    app: App,
    extensionLoader: ExtensionLoader
) : ViewModel() {

    var id: String? = null
    var data: PagedData<Shelf>? = null
    var title: String? = null

    private val music = extensionLoader.music
    private val throwFlow = app.throwFlow
    val feed = MutableStateFlow(PagingUtils.Data<Shelf>(null, null, null, null))

    var job = MutableStateFlow<Job?>(null)
    fun load() {
        job.value?.cancel()
        job.value = viewModelScope.launch(Dispatchers.IO) {
            val extension = music.getExtension(id) ?: return@launch
            feed.value = PagingUtils.Data(extension, title, data, null)
            data?.toFlow(extension)?.collectWith(throwFlow) {
                feed.value = PagingUtils.Data(extension, title, data, it)
            }
        }
    }

    fun initialize(model: ShelfViewModel) {
        if (id != null) return
        id = model.id
        data = model.data
        title = model.title
        load()
    }
}