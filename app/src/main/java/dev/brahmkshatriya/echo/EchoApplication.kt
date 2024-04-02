package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.AMOLED_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.THEME_KEY
import javax.inject.Inject

@HiltAndroidApp
class EchoApplication : Application() {

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        applyUiChanges(this, preferences)
    }

    companion object {
        @SuppressLint("RestrictedApi")
        fun applyUiChanges(app: Application, preferences: SharedPreferences) {
            val theme = when (preferences.getBoolean(AMOLED_KEY, false)) {
                true -> R.style.Amoled
                false -> null
            }

//            val blue = app.resources.getColor(R.color.blue, app.theme)

            val options = DynamicColorsOptions.Builder()
                .setOnAppliedCallback { activity ->
                    theme?.let { ThemeUtils.applyThemeOverlay(activity, it) }
                }
                .build()
            DynamicColors.applyToActivitiesIfAvailable(app, options)

            when (preferences.getString(THEME_KEY, "system")) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}