package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.common.ControllerExtension
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.MessagePostClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.ControllerClientsProvider
import dev.brahmkshatriya.echo.common.providers.LyricsClientsProvider
import dev.brahmkshatriya.echo.common.providers.MusicClientsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerClientsProvider
import dev.brahmkshatriya.echo.db.ExtensionDao
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.extensions.plugger.FileChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.PackageChangeListener
import dev.brahmkshatriya.echo.offline.BuiltInExtensionRepo
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ExtensionLoader(
    context: Context,
    offlineExtension: OfflineExtension,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
    private val extensionDao: ExtensionDao,
    private val userDao: UserDao,
    private val settings: SharedPreferences,
    private val refresher: MutableSharedFlow<Boolean>,
    private val userFlow: MutableSharedFlow<UserEntity?>,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    private val controllerListFlow: MutableStateFlow<List<ControllerExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    private val extensionFlow: MutableStateFlow<MusicExtension?>,
) {
    private val scope = MainScope() + CoroutineName("ExtensionLoader")
    private val listener = PackageChangeListener(context)
    val fileListener = FileChangeListener(scope)
    private val builtIn = BuiltInExtensionRepo(offlineExtension)

    private val musicExtensionRepo = MusicExtensionRepo(context, listener, fileListener, builtIn)
    private val trackerExtensionRepo = TrackerExtensionRepo(context, listener, fileListener)
    private val controllerExtensionRepo = ControllerExtensionRepo(context, listener, fileListener)
    private val lyricsExtensionRepo = LyricsExtensionRepo(context, listener, fileListener)

    val trackers = trackerListFlow
    val controllers = controllerListFlow
    val extensions = extensionListFlow
    val current = extensionFlow
    val currentWithUser = MutableStateFlow<Pair<MusicExtension?, UserEntity?>>(null to null)

    val priorityMap = ExtensionType.entries.associateWith {
        val key = it.priorityKey()
        val list = settings.getString(key, null).orEmpty().split(',')
        MutableStateFlow(list)
    }

    fun initialize() {
        scope.launch {
            getAllPlugins(scope)

            //Inject User to Music Extension
            launch {
                currentWithUser.collect { (musicExtension, user) ->
                    musicExtension ?: return@collect
                    setLoginUser(musicExtension, user, userFlow, throwableFlow)
                }
            }
            var extension: MusicExtension? = null
            var userEntities: List<UserEntity> = listOf()
            fun update() {
                val musicExtension = extension
                val user = userEntities.find { it.clientId == extension?.id }
                currentWithUser.value = musicExtension to user
            }
            launch {
                userDao.observeCurrentUser().collect {
                    userEntities = it
                    update()
                }
            }
            launch {
                extensionFlow.collect {
                    extension = it
                    update()
                }
            }

            //Inject other extensions
            launch {
                val combined = merge(
                    extensionFlow.map { listOfNotNull(it) }, trackerListFlow, lyricsListFlow, controllerListFlow
                )
                combined.collect { list ->
                    val trackerExtensions = trackerListFlow.value.orEmpty()
                    val controllerExtensions = controllerListFlow.value.orEmpty()
                    val lyricsExtensions = lyricsListFlow.value.orEmpty()
                    val musicExtensions = extensionListFlow.value.orEmpty()
                    list?.forEach { extension ->
                        extension.get<TrackerClientsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredTrackerClients, trackerExtensions) {
                                setTrackerExtensions(it)
                            }
                        }
                        extension.get<ControllerClientsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredControllerClients, controllerExtensions) {
                                setControllerExtensions(it)
                            }
                        }
                        extension.get<LyricsClientsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredLyricsClients, lyricsExtensions) {
                                setLyricsExtensions(it)
                            }
                        }
                        extension.get<MusicClientsProvider, Unit>(throwableFlow) {
                            inject(extension.name, requiredMusicClients, musicExtensions) {
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
        val controllers = MutableStateFlow<Unit?>(null)
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
            controllerExtensionRepo.getPlugins { list ->
                val controllerExtensions = list.map { (metadata, client) ->
                    ControllerExtension(metadata, client)
                }
                controllerListFlow.value = controllerExtensions
                controllerExtensions.setExtensions()
                controllers.emit(Unit)
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
        controllers.first { it != null }

        scope.launch {
            musicExtensionRepo.getPlugins { list ->
                val extensions = list.map { (metadata, client) ->
                    MusicExtension(metadata, client)
                }
                extensionListFlow.value = extensions
                val id = settings.getString(LAST_EXTENSION_KEY, null)
                val extension = extensions.find { it.metadata.id == id } ?: extensions.firstOrNull()
                setupMusicExtension(
                    scope, settings, extensionFlow, userDao, userFlow, throwableFlow, mutableMessageFlow, extension
                )
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
                setExtension(userDao, userFlow, throwableFlow, mutableMessageFlow, it)
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
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
            extension: MusicExtension?
        ) {
            settings.edit().putString(LAST_EXTENSION_KEY, extension?.id).apply()
            extension?.takeIf { it.metadata.enabled } ?: return
            scope.launch {
                setExtension(userDao, userFlow, throwableFlow, mutableMessageFlow, extension)
                extensionFlow.value = extension
            }
        }

        suspend fun setExtension(
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
            extension: Extension<*>,
        ) = withContext(Dispatchers.IO) {
            extension.get<MessagePostClient, Unit>(throwableFlow){
                registerMessagePostClient(this, this@withContext, mutableMessageFlow)
            }
            extension.run(throwableFlow) {
                withTimeout(TIMEOUT) { onExtensionSelected() }
            }
            val user = userDao.getCurrentUser(extension.id)
            setLoginUser(extension, user, userFlow, throwableFlow)
        }

        suspend fun setLoginUser(
            extension: Extension<*>,
            user: UserEntity?,
            flow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
        ) = withContext(Dispatchers.IO) {
            val success = extension.get<LoginClient, Unit>(throwableFlow) {
                withTimeout(TIMEOUT) { onSetLoginUser(user?.toUser()) }
            }
            if (success != null) flow.emit(user)
        }

        private fun registerMessagePostClient(
            client: MessagePostClient,
            scope: CoroutineScope,
            mutableMessageFlow: MutableSharedFlow<SnackBar.Message>
        ) {
            client.postMessage = { message ->
                scope.launch(Dispatchers.Main) {
                    mutableMessageFlow.emit(SnackBar.Message(message))
                }
            }
        }
    }
}