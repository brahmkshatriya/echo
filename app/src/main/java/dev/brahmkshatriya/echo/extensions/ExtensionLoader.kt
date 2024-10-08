package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.LyricsClientsProvider
import dev.brahmkshatriya.echo.common.providers.MusicClientsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerClientsProvider
import dev.brahmkshatriya.echo.db.ExtensionDao
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.extensions.plugger.AndroidPluginLoader
import dev.brahmkshatriya.echo.extensions.plugger.ApkFileManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.extensions.plugger.FilePluginSource
import dev.brahmkshatriya.echo.extensions.plugger.LazyPluginRepo
import dev.brahmkshatriya.echo.extensions.plugger.LazyPluginRepoImpl
import dev.brahmkshatriya.echo.extensions.plugger.LazyRepoComposer
import dev.brahmkshatriya.echo.extensions.plugger.PackageChangeListener
import dev.brahmkshatriya.echo.offline.LocalExtensionRepo
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.utils.catchWith
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class ExtensionLoader(
    context: Context,
    offlineExtension: OfflineExtension,
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
    private val scope = MainScope() + CoroutineName("ExtensionLoader")

    private fun Context.getPluginFileDir() = File(filesDir, "extensions").apply { mkdirs() }
    private val listener = PackageChangeListener(context)
    private fun <T : Any> getComposed(
        context: Context,
        suffix: String,
        vararg repo: LazyPluginRepo<Metadata, T>
    ): LazyPluginRepo<Metadata, T> {
        val loader = AndroidPluginLoader<T>(context)
        val apkFilePluginRepo = LazyPluginRepoImpl(
            FilePluginSource(context.getPluginFileDir(), ".eapk"),
            ApkFileManifestParser(context.packageManager, ApkManifestParser(ImportType.Apk)),
            loader,
        )
        val appPluginRepo = LazyPluginRepoImpl(
            ApkPluginSource(listener, context, "dev.brahmkshatriya.echo.$suffix"),
            ApkManifestParser(ImportType.App),
            loader
        )
        return LazyRepoComposer(appPluginRepo, apkFilePluginRepo, *repo)
    }

    private val musicExtensionRepo = MusicExtensionRepo(
        context,
        getComposed(context, "music", LocalExtensionRepo(offlineExtension))
    )

    private val trackerExtensionRepo = TrackerExtensionRepo(
        context, getComposed(context, "tracker")
    )

    private val lyricsExtensionRepo = LyricsExtensionRepo(
        context, getComposed(context, "lyrics")
    )

    val trackers = trackerListFlow
    val extensions = extensionListFlow
    val current = extensionFlow
    val currentWithUser = MutableStateFlow<Pair<MusicExtension?, UserEntity?>>(null to null)

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
                    extensionFlow.map { listOfNotNull(it) }, trackerListFlow, lyricsListFlow
                )
                combined.collect { list ->
                    val trackerExtensions = trackerListFlow.value.orEmpty()
                    val lyricsExtensions = lyricsListFlow.value.orEmpty()
                    val musicExtensions = extensionListFlow.value.orEmpty()
                    list?.forEach { extension ->
                        extension.get<TrackerClientsProvider, Unit>(throwableFlow) {
                            inject(requiredTrackerClients, trackerExtensions) {
                                setTrackerExtensions(it)
                            }
                        }
                        extension.get<LyricsClientsProvider, Unit>(throwableFlow) {
                            inject(requiredLyricsClients, lyricsExtensions) {
                                setLyricsExtensions(it)
                            }
                        }
                        extension.get<MusicClientsProvider, Unit>(throwableFlow) {
                            inject(requiredMusicClients, musicExtensions) {
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
        required: List<String>,
        extensions: List<R>,
        set: T.(List<R>) -> Unit
    ) {
        if (required.isEmpty()) set(extensions)
        else {
            val filtered = extensions.filter { it.metadata.id in required }
            if (filtered.size == required.size) set(filtered)
            else throw RequiredExtensionsException(required)
        }
    }

    private suspend fun getAllPlugins(scope: CoroutineScope) {
        val trackers = MutableStateFlow<Unit?>(null)
        val lyrics = MutableStateFlow<Unit?>(null)
        val music = MutableStateFlow<Unit?>(null)
        scope.launch {
            trackerExtensionRepo.getPlugins(ExtensionType.TRACKER) { list ->
                val trackerExtensions = list.map { (metadata, client) ->
                    TrackerExtension(metadata, client)
                }
                trackerListFlow.value = trackerExtensions
                trackerExtensions.setExtensions()
                trackers.emit(Unit)
            }
        }
        scope.launch {
            lyricsExtensionRepo.getPlugins(ExtensionType.LYRICS) { list ->
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
            musicExtensionRepo.getPlugins(ExtensionType.MUSIC) { list ->
                val extensions = list.map { (metadata, client) ->
                    MusicExtension(metadata, client)
                }
                extensionListFlow.value = extensions
                val id = settings.getString(LAST_EXTENSION_KEY, null)
                val extension = extensions.find { it.metadata.id == id } ?: extensions.firstOrNull()
                setupMusicExtension(
                    scope, settings, extensionFlow, userDao, userFlow, throwableFlow, extension
                )
                refresher.emit(false)
                music.emit(Unit)
            }
        }
        music.first { it != null }
    }

    private suspend fun <T : Any> LazyPluginRepo<Metadata, T>.getPlugins(
        type: ExtensionType, collector: FlowCollector<List<Pair<Metadata, Lazy<Result<T>>>>>
    ) = getAllPlugins().catchWith(throwableFlow).map { list ->
        list.mapNotNull { result ->
            val (metadata, client) = result.getOrElse {
                val error = it.cause ?: it
                throwableFlow.emit(ExtensionException(type, error))
                null
            } ?: return@mapNotNull null
            val metadataEnabled = isExtensionEnabled(type, metadata)
            Pair(metadataEnabled, client)
        }
    }.collect(collector)

    private suspend fun List<Extension<*>>.setExtensions() = coroutineScope {
        map {
            async {
                setExtension(userDao, userFlow, throwableFlow, it)
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

        fun setupMusicExtension(
            scope: CoroutineScope,
            settings: SharedPreferences,
            extensionFlow: MutableStateFlow<MusicExtension?>,
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: MusicExtension?
        ) {
            settings.edit().putString(LAST_EXTENSION_KEY, extension?.id).apply()
            extension?.takeIf { it.metadata.enabled } ?: return
            scope.launch {
                setExtension(userDao, userFlow, throwableFlow, extension)
                extensionFlow.value = extension
            }
        }

        suspend fun setExtension(
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: Extension<*>,
        ) = withContext(Dispatchers.IO) {
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
    }
}