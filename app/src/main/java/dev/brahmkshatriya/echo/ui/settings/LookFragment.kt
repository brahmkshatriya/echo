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
import dev.brahmkshatriya.echo.utils.ColorListPreference
import dev.brahmkshatriya.echo.utils.restartApp
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack


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

            val message = SnackBar.Message(
                getString(R.string.restart_app),
                SnackBar.Action(getString(R.string.restart)) {
                    context.restartApp()
                }
            )

            fun uiListener(block: (Any) -> Unit = {}) =
                Preference.OnPreferenceChangeListener { _, new ->
                    val activity = requireActivity()
                    applyUiChanges(activity.application, preferences)
                    createSnack(message)
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

                    entries = context.resources.getStringArray(R.array.themes)
                    entryValues = arrayOf("light", "dark", "system")
                    value = preferences.getString(THEME_KEY, "system")
                    onPreferenceChangeListener = uiListener()
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = CUSTOM_THEME_KEY
                    title = getString(R.string.custom_theme_color)
                    summary = getString(R.string.custom_theme_color_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    onPreferenceChangeListener = uiListener {
                        screen.findPreference<Preference>(COLOR_KEY)?.isEnabled = it as Boolean
                    }
                    addPreference(this)
                }

                ColorListPreference(this@LookPreference).apply {
                    key = COLOR_KEY
                    isEnabled = preferences.getBoolean(CUSTOM_THEME_KEY, false)
                    listener = ColorListPreference.Listener { createSnack(message) }
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

            PreferenceCategory(context).apply {
                title = getString(R.string.animation)
                key = "animation"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = ANIMATIONS_KEY
                    title = getString(R.string.animations)
                    summary = getString(R.string.animations_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    onPreferenceChangeListener = uiListener()
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SHARED_ELEMENT_KEY
                    title = getString(R.string.shared_element_transitions)
                    summary = getString(R.string.shared_element_transitions_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    onPreferenceChangeListener = uiListener()
                    addPreference(this)
                }
            }
        }
    }


    companion object {
        const val THEME_KEY = "theme"
        const val CUSTOM_THEME_KEY = "custom_theme"
        const val COLOR_KEY = "theme_color"
        const val AMOLED_KEY = "amoled"
        const val DYNAMIC_PLAYER = "dynamic_player"
        const val ANIMATIONS_KEY = "animations"
        const val SHARED_ELEMENT_KEY = "shared_element_transitions"
    }
}