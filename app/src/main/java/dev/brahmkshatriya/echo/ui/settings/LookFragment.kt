package dev.brahmkshatriya.echo.ui.settings

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.MainApplication.Companion.AMOLED_KEY
import dev.brahmkshatriya.echo.MainApplication.Companion.COLOR_KEY
import dev.brahmkshatriya.echo.MainApplication.Companion.CUSTOM_THEME_KEY
import dev.brahmkshatriya.echo.MainApplication.Companion.THEME_KEY
import dev.brahmkshatriya.echo.MainApplication.Companion.applyUiChanges
import dev.brahmkshatriya.echo.MainApplication.Companion.defaultColor
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.NAVBAR_GRADIENT
import dev.brahmkshatriya.echo.ui.player.PlayerFragment.Companion.DYNAMIC_PLAYER
import dev.brahmkshatriya.echo.ui.player.PlayerFragment.Companion.SHOW_BACKGROUND
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.prefs.ColorListPreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.ANIMATIONS_KEY
import dev.brahmkshatriya.echo.utils.ui.GradientDrawable.BACKGROUND_GRADIENT


class LookFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.look_and_feel)
    override val creator = { LookPreference() }

    class LookPreference : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val preferences = preferenceManager.sharedPreferences ?: return

            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            PreferenceCategory(context).apply {
                title = getString(R.string.ui)
                key = "ui"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = THEME_KEY
                    title = getString(R.string.theme)
                    summary = getString(R.string.theme_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false

                    entries = context.resources.getStringArray(R.array.themes)
                    entryValues = arrayOf("light", "dark", "system")
                    value = preferences.getString(THEME_KEY, "system")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = CUSTOM_THEME_KEY
                    title = getString(R.string.custom_theme_color)
                    summary = getString(R.string.custom_theme_color_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, it ->
                        screen.findPreference<Preference>(COLOR_KEY)?.isEnabled = it as Boolean
                        true
                    }
                    addPreference(this)
                }

                ColorListPreference(this@LookPreference).apply {
                    key = COLOR_KEY
                    setDefaultValue(context.defaultColor())
                    isEnabled = preferences.getBoolean(CUSTOM_THEME_KEY, true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AMOLED_KEY
                    title = getString(R.string.amoled)
                    summary = getString(R.string.amoled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = NAVBAR_GRADIENT
                    title = getString(R.string.navbar_gradient)
                    summary = getString(R.string.navbar_gradient_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = BACKGROUND_GRADIENT
                    title = getString(R.string.background_gradient)
                    summary = getString(R.string.background_gradient_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
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

                SwitchPreferenceCompat(context).apply {
                    key = SHOW_BACKGROUND
                    title = getString(R.string.show_background)
                    summary = getString(R.string.show_background_summary)
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
                    addPreference(this)
                }

//                SwitchPreferenceCompat(context).apply {
//                    key = SHARED_ELEMENT_KEY
//                    title = getString(R.string.shared_element_transitions)
//                    summary = getString(R.string.shared_element_transitions_summary)
//                    layoutResource = R.layout.preference_switch
//                    isIconSpaceReserved = false
//                    setDefaultValue(true)
//                    addPreference(this)
//                }
            }
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                THEME_KEY, CUSTOM_THEME_KEY, COLOR_KEY, AMOLED_KEY -> {
                    requireActivity().applyUiChanges()
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences!!.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(
                listener
            )
        }
    }


    companion object {
        fun Activity.applyUiChanges() {
            applyUiChanges(application)
            recreate()
        }
    }
}