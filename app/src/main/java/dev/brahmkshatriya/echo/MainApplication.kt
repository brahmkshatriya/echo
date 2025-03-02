package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
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
import dev.brahmkshatriya.echo.di.DI
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.CoroutineUtils
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class MainApplication : Application(), KoinStartup, SingletonImageLoader.Factory {

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@MainApplication)
        workManagerFactory()
        modules(DI.appModule)
    }

    private val downloader by inject<Downloader>()

    override fun onCreate() {
        super.onCreate()
        CoroutineUtils.setDebug()
        applyUiChanges(this)
        downloader.start()
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

    companion object {
        const val THEME_KEY = "theme"
        const val AMOLED_KEY = "amoled"
        const val CUSTOM_THEME_KEY = "custom_theme"
        const val COLOR_KEY = "color"

        private var theme: Int? = null

        @SuppressLint("RestrictedApi")
        private val onAppliedCallback = DynamicColors.OnAppliedCallback {
            val theme = theme ?: return@OnAppliedCallback
            ThemeUtils.applyThemeOverlay(it, theme)
        }

        fun Context.defaultColor() =
            ContextCompat.getColor(this, R.color.ic_launcher_background)

        fun Context.isAmoled() = getSettings().getBoolean(AMOLED_KEY, false)

        fun applyUiChanges(app: Application) {
            val settings = app.getSettings()
            val mode = when (settings.getString(THEME_KEY, "system")) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)

            theme = if (settings.getBoolean(AMOLED_KEY, false)) R.style.Amoled else null

            val customColor = if (!settings.getBoolean(CUSTOM_THEME_KEY, true)) null
            else settings.getInt(COLOR_KEY, app.defaultColor()).takeIf { it != -1 }

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
    }
}