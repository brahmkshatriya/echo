package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import dev.brahmkshatriya.echo.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.LyricsExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MiscExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerExtensionsProvider
import dev.brahmkshatriya.echo.db.ExtensionDao
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.extensions.plugger.FileChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.PackageChangeListener
import dev.brahmkshatriya.echo.utils.catchWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(UnstableApi::class)
class ExtensionLoader(
    context: Context,
    cache: SimpleCache,
    messageFlow: MutableSharedFlow<Message>,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val extensionDao: ExtensionDao,
    private val userDao: UserDao,
    private val settings: SharedPreferences,
    private val refresher: MutableSharedFlow<Boolean>,
    private val userFlow: MutableSharedFlow<UserEntity?>,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    private val miscListFlow: MutableStateFlow<List<MiscExtension>?>,
    private val extensionFlow: MutableStateFlow<MusicExtension?>,
) {
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Extension Loader")
    private val listener = PackageChangeListener(context)
    val fileListener = FileChangeListener(scope)

    val offline by lazy { OfflineExtension(context, cache) }
    private val offlinePair: Pair<Metadata, Injectable<ExtensionClient>> =
        OfflineExtension.metadata to Injectable { offline }

    private val unified: Pair<Metadata, Injectable<ExtensionClient>> =
        UnifiedExtension.metadata to Injectable { UnifiedExtension(context) }

//    private val test: Pair<Metadata, Injectable<ExtensionClient>> =
//        TestExtension.metadata to Injectable { TestExtension() }

//    private val download: Pair<Metadata, Injectable<ExtensionClient>> =
//        DownloadExtension.metadata to Injectable { DownloadExtension(context) }

    private val musicExtensionRepo =
        MusicExtensionRepo(context, messageFlow, listener, fileListener, offlinePair, unified)

    private val trackerExtensionRepo =
        TrackerExtensionRepo(context, messageFlow, listener, fileListener)

    private val lyricsExtensionRepo =
        LyricsExtensionRepo(context, messageFlow, listener, fileListener)

    private val miscExtensionRepo =
        MiscExtensionRepo(context, messageFlow, listener, fileListener)

    val trackers = trackerListFlow
    val extensions = extensionListFlow
    val current = extensionFlow
    val currentWithUser = MutableStateFlow<Pair<MusicExtension?, UserEntity?>>(null to null)

    val priorityMap = ExtensionType.entries.associateWith {
        val key = it.priorityKey()
        val list = settings.getString(key, null).orEmpty().split(',')
        MutableStateFlow(list)
    }

    private suspend fun Extension<*>.setLoginUser(trigger: Boolean = false) {
        val user = userDao.getCurrentUser(id)
        inject<LoginClient>(throwableFlow) {
            println("$this Setting User: ${user?.name}")
            withTimeout(TIMEOUT) { onSetLoginUser(user?.toUser()) }
        }
        if (trigger) {
            if (current.value?.id == id) currentWithUser.value = current.value to user
            userFlow.emit(user)
        }
    }

    private val userMap = mutableMapOf<String, String?>()
    private fun CurrentUser?.setUser(extension: Extension<*>) {
        val last = userMap[extension.id]
        val id = this?.id ?: ""
        if (last == id) return
        userMap[extension.id] = id
        scope.launch { extension.setLoginUser(extension is MusicExtension) }
    }

    private val combined = extensionListFlow
        .combine(trackerListFlow) { a, b -> a.orEmpty() + b.orEmpty() }
        .combine(lyricsListFlow) { a, b -> a + b.orEmpty() }
        .combine(miscListFlow) { a, b -> a + b.orEmpty() }

    @kotlin.OptIn(FlowPreview::class)
    fun initialize() {
        scope.launch {
            getAllPlugins(scope)

            launch {
                extensionFlow.collect {
                    currentWithUser.value = it to userDao.getCurrentUser(it?.id)
                }
            }

            launch {
                userDao.observeCurrentUser()
                    .combine(combined) { it, ext -> it to ext }
                    .distinctUntilChanged()
                    .collect { (users, extensions) ->
                        extensions.map { ext ->
                            val user = users.find { it.clientId == ext.id }
                            user.setUser(ext)
                        }
                    }
            }

            //Inject other extensions
            launch {
                combined.debounce(100L).collect { list ->
                    val trackerExtensions = trackerListFlow.value.orEmpty()
                    val lyricsExtensions = lyricsListFlow.value.orEmpty()
                    val musicExtensions = extensionListFlow.value.orEmpty()
                    val miscExtensions = miscListFlow.value.orEmpty()
                    list.forEach { extension ->
                        extension.inject<TrackerExtensionsProvider>(throwableFlow) {
                            inject(extension.name, requiredTrackerExtensions, trackerExtensions) {
                                setTrackerExtensions(it)
                            }
                        }
                        extension.inject<LyricsExtensionsProvider>(throwableFlow) {
                            inject(extension.name, requiredLyricsExtensions, lyricsExtensions) {
                                setLyricsExtensions(it)
                            }
                        }
                        extension.inject<MusicExtensionsProvider>(throwableFlow) {
                            inject(extension.name, requiredMusicExtensions, musicExtensions) {
                                setMusicExtensions(it)
                            }
                        }
                        extension.inject<MiscExtensionsProvider>(throwableFlow) {
                            inject(extension.name, requiredMiscExtensions, miscExtensions) {
                                setMiscExtensions(it)
                            }
                        }
                    }
                }
            }
        }

        // Refresh Extensions
        scope.launch {
            refresher.collect {
                if (it) launch {
                    getAllPlugins(scope)
                }
            }
        }
    }

    private fun <T, R : Extension<*>> T.inject(
        name: String,
        required: List<String>,
        extensions: List<R>,
        set: T.(List<R>) -> Unit
    ) {
        if (required.isEmpty()) set(extensions)
        else {
            val filtered = extensions.filter { it.metadata.id in required }
            if (filtered.size == required.size) set(filtered)
            else throw RequiredExtensionsException(name, required)
        }
    }

    private suspend fun getAllPlugins(scope: CoroutineScope) {
        val music = MutableStateFlow<Unit?>(null)
        scope.launch {
            musicExtensionRepo.getPlugins { list ->
                val extensions = list.map { (metadata, client) ->
                    MusicExtension(metadata, client)
                }
                extensionListFlow.value = extensions
                extensions.setExtensions()

                val id = settings.getString(LAST_EXTENSION_KEY, null)
                val extension = extensions.find { it.metadata.id == id } ?: extensions.firstOrNull()
                setupMusicExtension(scope, settings, extensionFlow, throwableFlow, extension)
                refresher.emit(false)
                music.emit(Unit)
            }
        }

        scope.launch(Dispatchers.IO) {
            trackerExtensionRepo.getPlugins { list ->
                val trackerExtensions = list.map { (metadata, client) ->
                    TrackerExtension(metadata, client)
                }
                trackerListFlow.value = trackerExtensions
                trackerExtensions.setExtensions()
            }
        }
        scope.launch(Dispatchers.IO) {
            lyricsExtensionRepo.getPlugins { list ->
                val lyricsExtensions = list.map { (metadata, client) ->
                    LyricsExtension(metadata, client)
                }
                lyricsListFlow.value = lyricsExtensions
                lyricsExtensions.setExtensions()
            }
        }
        scope.launch(Dispatchers.IO) {
            miscExtensionRepo.getPlugins { list ->
                val miscExtensions = list.map { (metadata, client) ->
                    MiscExtension(metadata, client)
                }
                miscListFlow.value = miscExtensions
                miscExtensions.setExtensions()
            }
        }

        music.first { it != null }
        userMap.clear()
    }

    private suspend fun <T : ExtensionClient> ExtensionRepo<T>.getPlugins(
        collector: FlowCollector<List<Pair<Metadata, Injectable<T>>>>
    ) {
        val pluginFlow = getAllPlugins().catchWith(throwableFlow).map { list ->
            list.mapNotNull { result ->
                val (metadata, client) = result.getOrElse {
                    val error = it.cause ?: it
                    throwableFlow.emit(ExtensionLoadingException(type, error))
                    null
                } ?: return@mapNotNull null
                val metadataEnabled = isExtensionEnabled(type, metadata)
                Pair(metadataEnabled, client)
            }
        }
        val priorityFlow = priorityMap[type]!!
        pluginFlow.combine(priorityFlow) { list, set ->
            list.sortedBy { set.indexOf(it.first.id) }
        }.collect(collector)
    }

    private suspend fun List<Extension<*>>.setExtensions() = coroutineScope {
        map { async { setExtension(throwableFlow, it) } }.awaitAll()
    }

    private suspend fun isExtensionEnabled(type: ExtensionType, metadata: Metadata) =
        withContext(Dispatchers.IO) {
            extensionDao.getExtension(type, metadata.id)?.enabled
                ?.let { metadata.copy(enabled = it) } ?: metadata
        }

    companion object {
        const val LAST_EXTENSION_KEY = "last_extension"
        private const val TIMEOUT = 5000L

        fun ExtensionType.priorityKey() = "priority_$this"

        fun setupMusicExtension(
            scope: CoroutineScope,
            settings: SharedPreferences,
            extensionFlow: MutableStateFlow<MusicExtension?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: MusicExtension?
        ) {
            settings.edit().putString(LAST_EXTENSION_KEY, extension?.id).apply()
            extension?.takeIf { it.metadata.enabled } ?: return
            scope.launch {
                extension.run(throwableFlow) {
                    withTimeout(TIMEOUT) { onExtensionSelected() }
                }
                extensionFlow.value = extension
            }
        }

        suspend fun setExtension(
            throwableFlow: MutableSharedFlow<Throwable>, extension: Extension<*>
        ) = withContext(Dispatchers.IO) {
            extension.inject<ExtensionClient>(throwableFlow) { onInitialize() }
        }
    }
}