package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadFragment
import dev.brahmkshatriya.echo.ui.extensions.manage.ManageExtensionsFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference


class SettingsFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.settings)
    override val icon get() = R.drawable.ic_settings_outline.toResourceImageHolder()
    override val creator = { SettingsPreference() }

    class SettingsPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            fun Preference.add(block: Preference.() -> Unit = {}) {
                block()
                layoutResource = R.layout.preference
                screen.addPreference(this)
            }

            TransitionPreference(context).add {
                title = getString(R.string.look_and_feel)
                key = "look"
                summary = getString(R.string.look_and_feel_summary)
                icon = AppCompatResources.getDrawable(context, R.drawable.ic_palette)
            }

            TransitionPreference(context).add {
                title = getString(R.string.audio)
                key = "audio"
                summary = getString(R.string.audio_summary)
                icon = AppCompatResources.getDrawable(context, R.drawable.ic_queue_music)
            }

            TransitionPreference(context).add {
                title = getString(R.string.extensions)
                key = "manage_extensions"
                summary = getString(R.string.extensions_summary)
                icon = AppCompatResources.getDrawable(context, R.drawable.ic_extension)
            }

            TransitionPreference(context).add {
                title = getString(R.string.downloads)
                key = "downloads"
                summary = getString(R.string.downloads_summary)
                icon = AppCompatResources.getDrawable(context, R.drawable.ic_download_for_offline)
            }

            TransitionPreference(context).add {
                title = getString(R.string.misc)
                key = "misc"
                summary = getString(R.string.misc_summary)
                icon = AppCompatResources.getDrawable(context, R.drawable.ic_info)
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            val view = listView.findViewById<View>(preference.key.hashCode())
            when (preference.key) {
                "misc" -> parentFragment?.openFragment<MiscFragment>(view)
                "audio" -> parentFragment?.openFragment<AudioFragment>(view)
                "manage_extensions" -> parentFragment?.openFragment<ManageExtensionsFragment>(view)
                "look" -> parentFragment?.openFragment<LookFragment>(view)
                "downloads" -> parentFragment?.openFragment<DownloadFragment>(view)
                else -> return false
            }
            return true
        }
    }

}

