package dev.brahmkshatriya.echo.extensions

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.helpers.WebViewClient
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.db.ExtensionDatabase
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File

@OptIn(UnstableApi::class)
class ExtensionLoader(
    val app: App,
    val db: ExtensionDatabase,
    private val cache: SimpleCache,
) {
    val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("ExtensionLoader")

    val fileIgnoreFlow = MutableSharedFlow<File?>()
    val extensions = Extensions(app.settings, scope, app.throwFlow)
    val updater = Updater(this)

    private val extensionRepo = ExtensionsRepo(
        this,
        UnifiedExtension.metadata to Injectable { UnifiedExtension(app.context, cache) },
        OfflineExtension.metadata to Injectable { OfflineExtension(app.context) },
//        TestExtension.metadata to Injectable { TestExtension() },
//        DownloadExtension.metadata to Injectable { DownloadExtension(app.context) }
//        TrackerTestExtension.metadata to Injectable { TrackerTestExtension() },
    )

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

    val webViewClient = WebViewClientImpl(app.context)
    fun createWebClient(metadata: Metadata): WebViewClient {
        if (metadata.type != ExtensionType.MUSIC)
            throw Exception("Webview client is not available for ${metadata.type} Extensions")
        return webViewClient.createFor(metadata)
    }

    init {
        loadExtensions()
    }
}