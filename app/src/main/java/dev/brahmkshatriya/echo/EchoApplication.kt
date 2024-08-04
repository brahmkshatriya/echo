package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.providers.LyricsClientsProvider
import dev.brahmkshatriya.echo.common.providers.MusicClientsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerClientsProvider
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.plugger.ExtensionException
import dev.brahmkshatriya.echo.plugger.ExtensionInfo
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.LyricsExtensionRepo
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.MusicExtensionRepo
import dev.brahmkshatriya.echo.plugger.RequiredExtensionsException
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtensionRepo
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.AMOLED_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.COLOR_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.CUSTOM_THEME_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.THEME_KEY
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel.Companion.tryWith
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel.Companion.setLoginUser
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
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject


@HiltAndroidApp
class EchoApplication : Application() {

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var refresher: MutableSharedFlow<Boolean>

    @Inject
    lateinit var musicExtensionRepo: MusicExtensionRepo

    @Inject
    lateinit var extensionListFlow: MutableStateFlow<List<MusicExtension>?>

    @Inject
    lateinit var extensionFlow: MutableStateFlow<MusicExtension?>

    @Inject
    lateinit var trackerExtensionRepo: TrackerExtensionRepo

    @Inject
    lateinit var trackerListFlow: MutableStateFlow<List<TrackerExtension>?>

    @Inject
    lateinit var lyricsExtensionRepo: LyricsExtensionRepo

    @Inject
    lateinit var lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    private val scope = MainScope() + CoroutineName("Application")

    override fun onCreate() {
        super.onCreate()
        //UI
        applyUiChanges(this, settings)

        //Crash Handling
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            exception.printStackTrace()
            ExceptionActivity.start(this, exception)
            Runtime.getRuntime().exit(0)
        }

        scope.launch {
            throwableFlow.collect {
                it.printStackTrace()
            }
        }

        //Extension Loading
        scope.launch {
            getAllPlugins()

            //Inject User
            launch {
                userDao.observeCurrentUser().collect { list ->
                    val extension = extensionFlow.value ?: return@collect
                    val (metadata, client) = extension
                    val user = list.find { it.clientId == metadata.id }
                    if (metadata.id == user?.clientId) setLoginUser(
                        extension.info, client, userDao, userFlow, throwableFlow
                    )
                }
            }
            //Inject other extensions
            launch {
                val combined = merge(
                    extensionListFlow,
                    trackerListFlow,
                    lyricsListFlow
                )
                combined.collect { list ->
                    val trackerClients =
                        trackerListFlow.value?.filter { it.metadata.enabled }
                            ?.map { it.metadata.id to it.client }.orEmpty()
                    val lyricsClients =
                        lyricsListFlow.value?.filter { it.metadata.enabled }
                            ?.map { it.metadata.id to it.client }.orEmpty()
                    val musicClients =
                        extensionListFlow.value?.filter { it.metadata.enabled }
                            ?.map { it.metadata.id to it.client }.orEmpty()
                    list?.forEach { extension ->
                        val info = extension.info
                        val client = extension.client
                        tryWith(throwableFlow, info) {
                            if (client is TrackerClientsProvider) client.injectClients(
                                client.requiredTrackerClients,
                                trackerClients
                            ) { client.setTrackerClients(it) }
                            if (client is LyricsClientsProvider) client.injectClients(
                                client.requiredLyricsClients,
                                lyricsClients
                            ) { client.setLyricsClients(it) }
                            if (client is MusicClientsProvider) client.injectClients(
                                client.requiredMusicClients,
                                musicClients
                            ) { client.setMusicClients(it) }
                        }
                    }
                }
            }
        }

        // Refresh Extensions
        scope.launch {
            refresher.collect {
                if (it) launch {
                    getAllPlugins()
                }
            }
        }
    }

    private fun <T, R : ExtensionClient> T.injectClients(
        required: List<String>,
        clients: List<Pair<String, R>>,
        set: T.(List<Pair<String, R>>) -> Unit
    ) {
        if (required.isEmpty()) set(clients)
        else {
            val filtered = clients.filter { it.first in required }
            if (filtered.size == required.size) set(filtered)
            else throw RequiredExtensionsException(required)
        }
    }

    private suspend fun getAllPlugins() = coroutineScope {
        val trackers = MutableStateFlow<Unit?>(null)
        val lyrics = MutableStateFlow<Unit?>(null)
        val music = MutableStateFlow<Unit?>(null)
        launch {
            trackerExtensionRepo.getPlugins(ExtensionType.TRACKER) { list ->
                trackerListFlow.value = list.map { (metadata, client) ->
                    TrackerExtension(metadata, client)
                }
                list.setExtensions(ExtensionType.TRACKER)
                trackers.emit(Unit)
            }
        }

        launch {
            lyricsExtensionRepo.getPlugins(ExtensionType.LYRICS) { list ->
                lyricsListFlow.value = list.map { (metadata, client) ->
                    LyricsExtension(metadata, client)
                }
                list.setExtensions(ExtensionType.LYRICS)
                lyrics.emit(Unit)
            }
        }

        trackers.first { it != null }
        lyrics.first { it != null }

        launch {
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

    private suspend fun <T> PluginRepo<ExtensionMetadata, T>.getPlugins(
        type: ExtensionType,
        collector: FlowCollector<List<Pair<ExtensionMetadata, T>>>
    ) = getAllPlugins().catchWith(throwableFlow).map { list ->
        list.mapNotNull { result ->
            val (metadata, client) = result.getOrElse {
                val error = it.cause ?: it
                throwableFlow.emit(ExtensionException(type, error))
                null
            } ?: return@mapNotNull null
            val metadataEnabled = isExtensionEnabled(type, metadata)
            metadataEnabled to client
        }
    }.collect(collector)

    private suspend fun List<Pair<ExtensionMetadata, ExtensionClient>>.setExtensions(
        type: ExtensionType
    ) =
        coroutineScope {
            map {
                async {
                    val info = ExtensionInfo(it.first, type)
                    setExtension(userDao, userFlow, throwableFlow, info, it.second)
                }
            }.awaitAll()
        }

    @Inject
    lateinit var database: EchoDatabase

    @Inject
    lateinit var userFlow: MutableSharedFlow<UserEntity?>

    private val userDao by lazy { database.userDao() }
    private val extensionDao by lazy { database.extensionDao() }

    private suspend fun isExtensionEnabled(type: ExtensionType, metadata: ExtensionMetadata) =
        withContext(Dispatchers.IO) {
            extensionDao.getExtension(type, metadata.id)?.enabled
                ?.let { metadata.copy(enabled = it) } ?: metadata
        }

    companion object {

        const val LAST_EXTENSION_KEY = "last_extension"
        const val TIMEOUT = 5000L

        fun setupMusicExtension(
            scope: CoroutineScope,
            settings: SharedPreferences,
            extensionFlow: MutableStateFlow<MusicExtension?>,
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: MusicExtension?
        ) {
            settings.edit().putString(LAST_EXTENSION_KEY, extension?.metadata?.id).apply()
            extension?.takeIf { it.metadata.enabled } ?: return
            scope.launch {
                val info = extension.info
                setExtension(userDao, userFlow, throwableFlow, info, extension.client)
                extensionFlow.value = extension
            }
        }

        suspend fun setExtension(
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            info: ExtensionInfo,
            client: ExtensionClient
        ) = withContext(Dispatchers.IO) {
            tryWith(throwableFlow, info) {
                withTimeout(TIMEOUT) { client.onExtensionSelected() }
            }
            setLoginUser(info, client, userDao, userFlow, throwableFlow)
        }

        @SuppressLint("RestrictedApi")
        fun applyUiChanges(app: Application, preferences: SharedPreferences) {
            val theme = when (preferences.getBoolean(AMOLED_KEY, false)) {
                true -> R.style.Amoled
                false -> null
            }

            val customColor = if (!preferences.getBoolean(CUSTOM_THEME_KEY, false)) null
            else preferences.getInt(COLOR_KEY, -1).takeIf { it != -1 }

            val builder = if (customColor != null) DynamicColorsOptions.Builder()
                .setContentBasedSource(customColor)
            else DynamicColorsOptions.Builder()

            theme?.let {
                builder.setOnAppliedCallback {
                    ThemeUtils.applyThemeOverlay(it, theme)
                }
            }

            DynamicColors.applyToActivitiesIfAvailable(app, builder.build())

            when (preferences.getString(THEME_KEY, "system")) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}