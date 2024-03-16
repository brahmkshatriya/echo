package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R

class AboutFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = context.packageName
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        Preference(context).apply {
            val version = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
            title = getString(R.string.version)
            summary = version
            layoutResource = R.layout.preference
            isIconSpaceReserved = false
            isSelectable = false
            screen.addPreference(this)
        }
    }
}