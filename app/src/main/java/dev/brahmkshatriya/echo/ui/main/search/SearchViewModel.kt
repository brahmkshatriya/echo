package dev.brahmkshatriya.echo.ui.main.search

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.QuickSearchClient
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val app: App,
    extensionLoader: ExtensionLoader
) : ViewModel() {
    val queryFlow = MutableStateFlow("")
    private val music = extensionLoader.music
    private val _currentExtension = extensionLoader.current
    val currentExtension get() = _currentExtension.value
    val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())

    fun quickSearch(extensionId: String?, query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetExtId = extensionId ?: currentExtension?.id
            val list = if (query.isBlank()) {
                if (targetExtId != null) {
                    val extension = music.getExtension(targetExtId)
                    defaultQuickSearch(extension, app.context, "")
                } else {
                    getAllHistory()
                }
            } else {
                val extId = targetExtId ?: return@launch
                val extension = music.getExtension(extId)
                extension?.getIf<QuickSearchClient, List<QuickSearchItem>>(app.throwFlow) {
                    quickSearch(query)
                } ?: defaultQuickSearch(extension, app.context, query)
            }
            quickFeed.value = list
        }
    }

    private fun getAllHistory(): List<QuickSearchItem> {
        val context = app.context
        val extensions = music.value.filter { it.isEnabled }
        
        return extensions.flatMap { extension ->
            getHistory(extension.prefs(context)).map { query ->
                QuickSearchItem.Query(query, true, mapOf("extensionId" to extension.id))
            }
        }.distinctBy { it.title }.take(10)
    }

    fun deleteSearch(extensionId: String?, item: QuickSearchItem, query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Determine which extension's history to delete from
            val targetExtId = when {
                !extensionId.isNullOrBlank() -> extensionId
                item is QuickSearchItem.Query -> item.extras["extensionId"]
                else -> null
            } ?: currentExtension?.id ?: return@launch
            
            val extension = music.getExtension(targetExtId)
            defaultDeleteSearch(extension, app.context, item)
            
            // Refresh the quick search results
            val currentQuery = queryFlow.value
            val currentExtId = extensionId ?: currentExtension?.id
            quickSearch(currentExtId, currentQuery)
        }
    }

    companion object {
        fun defaultQuickSearch(
            extension: Extension<*>?, context: Context, query: String
        ): List<QuickSearchItem> {
            val setting = extension?.prefs(context) ?: return emptyList()
            if (query.isNotBlank()) return emptyList()
            return getHistory(setting).map { QuickSearchItem.Query(it, true) }
        }

        fun defaultDeleteSearch(extension: Extension<*>?, context: Context, item: QuickSearchItem) {
            val setting = extension?.prefs(context) ?: return
            val history = getHistory(setting).toMutableList()
            history.remove(item.title)
            setting.edit { putString("search_history", history.joinToString(",")) }
        }

        private fun getHistory(setting: SharedPreferences): List<String> {
            return setting.getString("search_history", "")
                ?.split(",")?.mapNotNull {
                    it.takeIf { it.isNotBlank() }
                }?.distinct()?.take(5)
                ?: emptyList()
        }

        fun Extension<*>.saveInHistory(context: Context, query: String) {
            if (query.isBlank()) return
            val setting = prefs(context)
            val history = getHistory(setting).toMutableList()
            history.add(0, query)
            setting.edit { putString("search_history", history.joinToString(",")) }
        }
    }
}