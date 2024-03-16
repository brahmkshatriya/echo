package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R

class LookFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = context.packageName
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        ListPreference(context).apply {
            key = THEME_KEY
            title = getString(R.string.theme)
            summary = getString(R.string.theme_summary)
            layoutResource = R.layout.preference
            isIconSpaceReserved = false

            entries = arrayOf("Light", "Dark", "System default")
            entryValues = arrayOf("light", "dark", "system")
            screen.addPreference(this)
        }
    }
    companion object {
        const val THEME_KEY = "theme"
    }
}