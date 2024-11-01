package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.prefs.MaterialSliderPreference

class DownloadFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.downloads)
    override val transitionName = "downloads"
    override val creator = { DownloadsPreference() }

    class DownloadsPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = context.packageName
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            MaterialSliderPreference(context, 1, 10, allowOverride = false).apply {
                key = DOWNLOAD_NUM
                title = getString(R.string.download_num)
                summary = getString(R.string.download_num_summary)
                isIconSpaceReserved = false
                setDefaultValue(2)
                screen.addPreference(this)
            }
        }

        companion object {
            const val DOWNLOAD_NUM = "download_num"
        }
    }
}