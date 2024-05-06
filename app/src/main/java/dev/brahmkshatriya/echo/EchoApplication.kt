package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject


@HiltAndroidApp
class EchoApplication : Application() {

    @Inject
    lateinit var settings: SharedPreferences

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
            musicExtensionRepo.getPlugins { list ->
                println("beuh : $list")
                val extensions = list.map { (metadata, client) ->
                    MusicExtension(metadata, client)
                }
                extensionListFlow.value = extensions
                val id = settings.getString(LAST_EXTENSION_KEY, null)
                val extension = extensions.find { it.metadata.id == id } ?: extensions.firstOrNull()
                setupExtension(extension)
            }
            trackerExtensionRepo.getPlugins { list ->
                trackerListFlow.value = list.map { TrackerExtension(it.first, it.second) }
            }
            lyricsExtensionRepo.getPlugins { list ->
                lyricsListFlow.value = list.map { LyricsExtension(it.first, it.second) }
            }
        }
    }

    private suspend fun <T> PluginRepo<ExtensionMetadata, T>.getPlugins(
        collector: FlowCollector<List<Pair<ExtensionMetadata, T>>>
    ) = coroutineScope {
        launch {
            getAllPlugins().catchWith(throwableFlow).map { list ->
                list.mapNotNull { result ->
                    println("wut : ${result.getOrNull()}")
                    tryWith(throwableFlow) {
                        result.getOrThrow()
                    }
                }
            }.collect(collector)
        }
    }

    @Inject
    lateinit var database: EchoDatabase

    @Inject
    lateinit var userFlow: MutableSharedFlow<UserEntity?>

    private val userDao by lazy { database.userDao() }

    private fun setupExtension(extension: MusicExtension?) {
        setExtension(scope, settings, extensionFlow, userDao, userFlow, throwableFlow, extension)
    }


    companion object {

        const val LAST_EXTENSION_KEY = "last_extension"

        fun setExtension(
            scope: CoroutineScope,
            settings: SharedPreferences,
            extensionFlow: MutableStateFlow<MusicExtension?>,
            userDao: UserDao,
            userFlow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: MusicExtension?
        ) {
            settings.edit().putString(LAST_EXTENSION_KEY, extension?.metadata?.id).apply()
            scope.launch(Dispatchers.IO) {
                extension?.run {
                    client.onExtensionSelected()
                    setLoginUser(metadata.id, client, userDao, userFlow, throwableFlow)
                }
                extensionFlow.value = extension
            }
        }

        @SuppressLint("RestrictedApi")
        fun applyUiChanges(app: Application, preferences: SharedPreferences) {
            val theme = when (preferences.getBoolean(AMOLED_KEY, false)) {
                true -> R.style.Amoled
                false -> null
            }

            val customColor = if (!preferences.getBoolean(CUSTOM_THEME_KEY, false)) null
            else preferences.getInt(COLOR_KEY, -1).takeIf { it != -1 }

            val builder = if (customColor != null)
                DynamicColorsOptions.Builder().setContentBasedSource(customColor)
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