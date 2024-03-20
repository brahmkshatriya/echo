package dev.brahmkshatriya.echo

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
import dev.brahmkshatriya.echo.ui.settings.LookFragment
import javax.inject.Inject

@HiltAndroidApp
class EchoApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .allowHardware(false)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(1024 * 1024 * 100) // 100MB
                    .build()
            }
            .crossfade(true)
            .build()
    }

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        applyUiChanges(this, preferences)
    }

    companion object {
        fun applyUiChanges(application: Application, preferences: SharedPreferences) {
            val theme = when (preferences.getBoolean(LookFragment.AMOLED_KEY, false)) {
                true -> R.style.Amoled_Theme_Echo
                false -> R.style.Default_Theme_Echo
            }

            val options = DynamicColorsOptions.Builder()
                .setThemeOverlay(theme)
                .build()
            DynamicColors.applyToActivitiesIfAvailable(application, options)

            when (preferences.getString(LookFragment.THEME_KEY, "system")) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}