package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
import dev.brahmkshatriya.echo.ExtensionOpenerActivity.Companion.cleanupTempApks
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
class EchoApplication : Application() {

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
        cleanupTempApks()

        //Crash Handling
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            exception.printStackTrace()
            ExceptionActivity.start(this, exception)
            Runtime.getRuntime().exit(0)
        }

        scope.launch {
            throwableFlow.collect {
                println(getTitle(it))
                println(getDetails(it))
            }
        }
    }

    companion object {

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
}