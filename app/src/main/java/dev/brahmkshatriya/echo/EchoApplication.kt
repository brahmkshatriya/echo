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
import dev.brahmkshatriya.echo.dao.UserDao
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.LyricsExtensionRepo
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.MusicExtensionRepo
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtensionRepo
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.AMOLED_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.COLOR_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.CUSTOM_THEME_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.THEME_KEY
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.tryWith
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
        }

        // Refresh Extensions
        scope.launch {
            refresher.collect {
                if (it) getAllPlugins()
            }
        }

        //User
        scope.launch {
            userDao.observeCurrentUser().collect { list ->
                val extension = extensionFlow.value ?: return@collect
                val (metadata, client) = extension
                val user = list.find { it.clientId == metadata.id }
                if (metadata.id == user?.clientId) setLoginUser(
                    metadata.id, client, userDao, userFlow, throwableFlow
                )
            }
        }
    }

    private suspend fun getAllPlugins() = coroutineScope {
        val trackers = MutableStateFlow<Unit?>(null)
        val lyrics = MutableStateFlow<Unit?>(null)
        launch {
            trackerExtensionRepo.getPlugins { list ->
                trackerListFlow.value = list.map { TrackerExtension(it.first, it.second) }
                list.map {
                    async {
                        setExtension(userDao, userFlow, throwableFlow, it.first, it.second)
                    }
                }.awaitAll()
                trackers.emit(Unit)
            }
        }

        launch {
            lyricsExtensionRepo.getPlugins { list ->
                lyricsListFlow.value = list.map { LyricsExtension(it.first, it.second) }
                list.map {
                    async {
                        setExtension(userDao, userFlow, throwableFlow, it.first, it.second)
                    }
                }.awaitAll()
                lyrics.emit(Unit)
            }
        }
        trackers.first { it != null }
        lyrics.first { it != null }

        musicExtensionRepo.getPlugins { list ->
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
        }
    }

    private suspend fun <T> PluginRepo<ExtensionMetadata, T>.getPlugins(
        collector: FlowCollector<List<Pair<ExtensionMetadata, T>>>
    ) = getAllPlugins().catchWith(throwableFlow).map { list ->
        list.mapNotNull { result ->
            tryWith(throwableFlow) {
                result.getOrElse {
                    throw it.cause!!
                }
            }
        }
    }.collect(collector)

    @Inject
    lateinit var database: EchoDatabase

    @Inject
    lateinit var userFlow: MutableSharedFlow<UserEntity?>

    private val userDao by lazy { database.userDao() }


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
            extension ?: return
            scope.launch {
                setExtension(userDao, userFlow, throwableFlow, extension.metadata, extension.client)
                extensionFlow.value = extension
            }
        }

        suspend fun setExtension(
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            metadata: ExtensionMetadata,
            client: ExtensionClient
        ) = withContext(Dispatchers.IO) {
            tryWith(throwableFlow) {
                withTimeout(TIMEOUT) { client.onExtensionSelected() }
            }
            setLoginUser(metadata.id, client, userDao, userFlow, throwableFlow)
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