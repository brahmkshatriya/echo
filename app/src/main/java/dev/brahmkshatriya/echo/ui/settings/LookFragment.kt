package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.EchoApplication.Companion.applyUiChanges
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.viewmodels.SnackBarViewModel.Companion.createSnack


class LookFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.look_and_feel)
    override val transitionName = "look"
    override val creator = { LookPreference() }

    class LookPreference : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = context.packageName
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val preferences = preferenceManager.sharedPreferences ?: return

            fun uiListener(block: (Any) -> Unit = {}) =
                Preference.OnPreferenceChangeListener { _, new ->
                    val activity = requireActivity()
                    applyUiChanges(activity.application, preferences)
                    createSnack(R.string.restart_app)
                    block(new)
                    true
                }

            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen


            PreferenceCategory(context).apply {
                title = getString(R.string.ui)
                key = "ui"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                ListPreference(context).apply {
                    key = THEME_KEY
                    title = getString(R.string.theme)
                    summary = getString(R.string.theme_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false

                    entries = arrayOf("Light", "Dark", "System default")
                    entryValues = arrayOf("light", "dark", "system")
                    value = preferences.getString(THEME_KEY, "system")
                    onPreferenceChangeListener = uiListener()
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AMOLED_KEY
                    title = getString(R.string.amoled)
                    summary = getString(R.string.amoled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    onPreferenceChangeListener = uiListener()
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = DYNAMIC_PLAYER
                    title = getString(R.string.dynamic_player)
                    summary = getString(R.string.dynamic_player_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }


        }
    }


    companion object {
        const val THEME_KEY = "theme"
        const val AMOLED_KEY = "amoled"
        const val DYNAMIC_PLAYER = "dynamic_player"
    }
}