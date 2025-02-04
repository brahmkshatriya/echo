package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
import dev.brahmkshatriya.echo.ExtensionOpenerActivity.Companion.cleanupTempApks
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.getDetails
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.getTitle
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.AMOLED_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.COLOR_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.CUSTOM_THEME_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.THEME_KEY
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

@HiltAndroidApp
class EchoApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var extensionLoader: ExtensionLoader

    @Inject
    lateinit var downloader: Downloader

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    private val scope = MainScope() + CoroutineName("Application")

    override fun onCreate() {
        super.onCreate()
        //UI
        applyLocale(settings)
        applyUiChanges(this, settings)

        //Extensions
        cleanupTempApks()
        extensionLoader.initialize()
        downloader.start()

//        //Crash Handling
//        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
//            exception.printStackTrace()
//            ExceptionActivity.start(this, exception)
//            Runtime.getRuntime().exit(0)
//        }

        scope.launch {
            throwableFlow.collect {
                println(getTitle(it))
                println(getDetails(it))
            }
        }
    }

    companion object {

        private var theme: Int? = null

        @SuppressLint("RestrictedApi")
        private val onAppliedCallback = DynamicColors.OnAppliedCallback {
            val theme = theme ?: return@OnAppliedCallback
            ThemeUtils.applyThemeOverlay(it, theme)
        }

        fun applyUiChanges(app: Application, preferences: SharedPreferences) {
            val mode = when (preferences.getString(THEME_KEY, "system")) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)

            theme = if (preferences.getBoolean(AMOLED_KEY, false)) R.style.Amoled else null

            val customColor = if (!preferences.getBoolean(CUSTOM_THEME_KEY, false)) null
            else preferences.getInt(COLOR_KEY, -1).takeIf { it != -1 }

            val builder = DynamicColorsOptions.Builder()
            builder.setOnAppliedCallback(onAppliedCallback)
            customColor?.let { builder.setContentBasedSource(it) }
            DynamicColors.applyToActivitiesIfAvailable(app, builder.build())
        }

        fun applyLocale(sharedPref: SharedPreferences) {
            val value = sharedPref.getString("language", "system") ?: "system"
            val locale = if (value == "system") LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(value)
            AppCompatDelegate.setApplicationLocales(locale)
        }

        fun Context.appVersion(): String = packageManager
            .getPackageInfo(packageName, 0)
            .versionName!!
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(1024 * 1024 * 100) // 100MB
                    .build()
            }
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

}