package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.LyricsExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerExtensionsProvider
import dev.brahmkshatriya.echo.db.ExtensionDao
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.extensions.plugger.FileChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.PackageChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.catchLazy
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.offline.UnifiedExtension
import dev.brahmkshatriya.echo.utils.catchWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val extensionDao: ExtensionDao,
    private val userDao: UserDao,
    private val settings: SharedPreferences,
    private val refresher: MutableSharedFlow<Boolean>,
    private val userFlow: MutableSharedFlow<UserEntity?>,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    private val extensionFlow: MutableStateFlow<MusicExtension?>,
) {
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Extension Loader")
    private val listener = PackageChangeListener(context)
    val fileListener = FileChangeListener(scope)

    val offline = OfflineExtension.metadata to catchLazy { OfflineExtension(context, cache) }
    private val unified = UnifiedExtension.metadata to catchLazy { UnifiedExtension(context) }
//    private val test = TestExtension.metadata to catchLazy { TestExtension() }

    private val musicExtensionRepo =
        MusicExtensionRepo(context, listener, fileListener, offline, unified)

    private val trackerExtensionRepo =
        TrackerExtensionRepo(context, listener, fileListener)

    private val lyricsExtensionRepo =
        LyricsExtensionRepo(context, listener, fileListener)

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
        println("Setting login user for $name")
        val user = userDao.getCurrentUser(id)
        get<LoginClient, Unit>(throwableFlow) {
            withTimeout(TIMEOUT) { onSetLoginUser(user?.toUser()) }
        }
        if (trigger) {
            if (current.value?.id == id) currentWithUser.value = current.value to user
            userFlow.emit(user)
        }
    }

    private val userMap = mutableMapOf<String, String?>()
    private suspend fun CurrentUser.setUser() {
        val last = userMap[clientId]
        if (last == id) return
        userMap[clientId] = id
        trackerListFlow.getExtension(clientId)?.setLoginUser()
        lyricsListFlow.getExtension(clientId)?.setLoginUser()
        extensionListFlow.getExtension(clientId)?.setLoginUser(true)
    }
    private val combined = extensionListFlow
        .combine(trackerListFlow) { a, b -> a.orEmpty() + b.orEmpty() }
        .combine(lyricsListFlow) { a, b -> a + b.orEmpty() }

    private var initialized = false
    fun initialize() {
        if (initialized) return
        initialized = true
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
                        val ext = extensions.map { it.id }.toSet()
                        val extToRemove = userMap.keys - ext
                        extToRemove.forEach { userMap.remove(it) }
                        users.filter { it.clientId in ext }.map { launch { it.setUser() } }
                    }
            }

            //Inject other extensions
            launch {
                combined.collect { list ->
                    val trackerExtensions = trackerListFlow.value.orEmpty()
                    val lyricsExtensions = lyricsListFlow.value.orEmpty()
                    val musicExtensions = extensionListFlow.value.orEmpty()
                    list.forEach { extension ->
                        extension.get<TrackerExtensionsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredTrackerExtensions, trackerExtensions) {
                                setTrackerExtensions(it)
                            }
                        }
                        extension.get<LyricsExtensionsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredLyricsExtensions, lyricsExtensions) {
                                setLyricsExtensions(it)
                            }
                        }
                        extension.get<MusicExtensionsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredMusicExtensions, musicExtensions) {
                                setMusicExtensions(it)
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
        val trackers = MutableStateFlow<Unit?>(null)
        val lyrics = MutableStateFlow<Unit?>(null)
        val music = MutableStateFlow<Unit?>(null)
        scope.launch {
            trackerExtensionRepo.getPlugins { list ->
                val trackerExtensions = list.map { (metadata, client) ->
                    TrackerExtension(metadata, client)
                }
                trackerListFlow.value = trackerExtensions
                trackerExtensions.setExtensions()
                trackers.emit(Unit)
            }
        }
        scope.launch {
            lyricsExtensionRepo.getPlugins { list ->
                val lyricsExtensions = list.map { (metadata, client) ->
                    LyricsExtension(metadata, client)
                }
                lyricsListFlow.value = lyricsExtensions
                lyricsExtensions.setExtensions()
                lyrics.emit(Unit)
            }
        }
        lyrics.first { it != null }
        trackers.first { it != null }

        scope.launch {
            musicExtensionRepo.getPlugins { list ->
                val extensions = list.map { (metadata, client) ->
                    MusicExtension(metadata, client)
                }
                extensionListFlow.value = extensions
                val id = settings.getString(LAST_EXTENSION_KEY, null)
                val extension = extensions.find { it.metadata.id == id } ?: extensions.firstOrNull()
                setupMusicExtension(scope, settings, extensionFlow, throwableFlow, extension)
                refresher.emit(false)
                music.emit(Unit)
            }
        }
        music.first { it != null }
    }

    private suspend fun <T : ExtensionClient> ExtensionRepo<T>.getPlugins(
        collector: FlowCollector<List<Pair<Metadata, Lazy<Result<T>>>>>
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
        map {
            async {
                setExtension(throwableFlow, it)
            }
        }.awaitAll()
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
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: Extension<*>,
        ) = withContext(Dispatchers.IO) {
            extension.run(throwableFlow) {
                withTimeout(TIMEOUT) { onExtensionSelected() }
            }
        }
    }
}