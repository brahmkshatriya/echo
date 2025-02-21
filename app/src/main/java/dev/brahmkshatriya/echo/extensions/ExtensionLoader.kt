package dev.brahmkshatriya.echo.extensions

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.db.ExtensionDatabase
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@OptIn(UnstableApi::class)
class ExtensionLoader(
    val app: App,
    val db: ExtensionDatabase,
    private val cache: SimpleCache,
) {
    val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("ExtensionLoader")

    val offline by lazy { OfflineExtension(app.context, cache) }
    private val extensionRepo = ExtensionsRepo(
        this,
        UnifiedExtension.metadata to Injectable { UnifiedExtension(app.context) },
        OfflineExtension.metadata to Injectable { offline },
//        TestExtension.metadata to Injectable { TestExtension() }
    )

    val extensions = Extensions(app.settings, scope, app.throwFlow)
    private var job: Job? = null
    private fun loadExtensions() {
        job?.cancel()
        extensions.flush()
        job = scope.launch {
            extensionRepo.getAllPlugins().collect {
                extensions.addAll(it)
            }
        }
    }

    fun refresh() {
        loadExtensions()
    }

    init {
        loadExtensions()
    }
}