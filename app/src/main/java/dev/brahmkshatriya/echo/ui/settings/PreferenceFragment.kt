package dev.brahmkshatriya.echo.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel


class SettingsFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.settings)
    override val transitionName = "settings"
    override val creator = { SettingsPreference() }
}

class SettingsPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = context.packageName
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        Preference(context).apply {
            title = getString(R.string.look_and_feel)
            key = "look"
            summary = getString(R.string.look_and_feel_summary)
            icon = AppCompatResources.getDrawable(context, R.drawable.ic_palette)
            layoutResource = R.layout.preference
            screen.addPreference(this)
        }

        Preference(context).apply {
            title = getString(R.string.audio)
            key = "audio"
            summary = getString(R.string.audio_summary)
            icon = AppCompatResources.getDrawable(context, R.drawable.ic_queue_music)
            layoutResource = R.layout.preference
            screen.addPreference(this)
        }

        Preference(context).apply {
            title = getString(R.string.extension_settings)
            key = "extension"
            summary = getString(R.string.extension_settings_summary)
            icon = AppCompatResources.getDrawable(context, R.drawable.ic_extension)
            layoutResource = R.layout.preference
            screen.addPreference(this)
        }

        Preference(context).apply {
            title = getString(R.string.about)
            key = "about"
            summary = getString(R.string.about_summary)
            icon = AppCompatResources.getDrawable(context, R.drawable.ic_info)
            layoutResource = R.layout.preference
            screen.addPreference(this)
        }
    }

    @SuppressLint("CommitTransaction")
    private fun start(transition: String, view: View, fragment: BaseSettingsFragment): Boolean {
        view.transitionName = transition
        requireActivity().supportFragmentManager
            .beginTransaction()
            .addSharedElement(view, transition)
            .add(R.id.settingsFragmentContainerView, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    private val extensionViewModel: ExtensionViewModel by activityViewModels()

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val view = listView.getChildAt(preference.order)
        fun start(fragment: BaseSettingsFragment) = start(preference.key, view, fragment)

        return when (preference.key) {
            "about" -> start(AboutFragment())
            "audio" -> start(AudioFragment())
            "extension" -> {
                val extension = extensionViewModel.currentExtension
                    ?: return false
                start(ExtensionFragment.newInstance(extension.metadata))
            }

            "look" -> start(LookFragment())
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}