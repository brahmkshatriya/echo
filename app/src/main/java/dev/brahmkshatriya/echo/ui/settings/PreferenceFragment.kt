package dev.brahmkshatriya.echo.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel

class PreferenceFragment : PreferenceFragmentCompat() {
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
    private fun startPreferenceFragment(title: CharSequence?, fragment: PreferenceFragmentCompat) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .add(R.id.settingsFragmentContainerView, SettingsFragment(title,fragment))
            .addToBackStack(null)
            .commit()
    }

    private val extensionViewModel: ExtensionViewModel by activityViewModels()

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "about" -> startPreferenceFragment(preference.title, AboutFragment())
            "audio" -> startPreferenceFragment(preference.title, AudioFragment())
            "extension" -> {
                val extension = extensionViewModel.getCurrentExtension() ?: return false
                startPreferenceFragment(extension.metadata.name, ExtensionFragment(extension))
            }
            "look" -> startPreferenceFragment(preference.title, LookFragment())
        }
        return super.onPreferenceTreeClick(preference)
    }
}