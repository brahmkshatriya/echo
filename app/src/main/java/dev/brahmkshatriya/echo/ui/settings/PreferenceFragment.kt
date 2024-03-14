package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = context.packageName
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        PreferenceCategory(context).apply {
            title = getString(R.string.general)
            key = "general"
            screen.addPreference(this)

            SwitchPreferenceCompat(context).apply {
                key = CLOSE_PLAYER
                title = getString(R.string.stop_player)
                summary = getString(R.string.stop_player_summary)
                setDefaultValue(false)
                addPreference(this)
            }

            SwitchPreferenceCompat(context).apply {
                key = AUTO_START_RADIO
                title = getString(R.string.auto_start_radio)
                summary = getString(R.string.auto_start_radio_summary)
                setDefaultValue(true)
                addPreference(this)
            }
        }

        PreferenceCategory(context).apply {
            title = getString(R.string.about)
            key = "about"
            screen.addPreference(this)

            Preference(context).apply {
                val version = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                title = getString(R.string.version)
                summary = version
                isSelectable = false
                addPreference(this)
            }
        }
    }

    companion object {
        const val CLOSE_PLAYER = "close_player_when_app_closes"
        const val AUTO_START_RADIO = "auto_start_radio"
    }
}