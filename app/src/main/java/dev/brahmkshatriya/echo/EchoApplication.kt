package dev.brahmkshatriya.echo

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.THEME_KEY
import javax.inject.Inject

@HiltAndroidApp
class EchoApplication : Application() , ImageLoaderFactory {
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

    @Inject lateinit var preferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        when (preferences.getString(THEME_KEY, "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}