package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R

class AudioFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.audio)
    override val transitionName = "audio"
    override val creator = { AudioPreference() }


    class AudioPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = context.packageName
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            PreferenceCategory(context).apply {
                title = getString(R.string.behavior)
                key = "behavior"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = CLOSE_PLAYER
                    title = getString(R.string.stop_player)
                    summary = getString(R.string.stop_player_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AUTO_START_RADIO
                    title = getString(R.string.auto_start_radio)
                    summary = getString(R.string.auto_start_radio_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                ListPreference(context).also {
                    it.key = STREAM_QUALITY
                    it.title = getString(R.string.stream_quality)
                    it.summary = getString(R.string.stream_quality_summary)
                    it.entries = context.resources.getStringArray(R.array.stream_qualities)
                    it.entryValues = streamQualities
                    layoutResource = R.layout.preference
                    it.isIconSpaceReserved = false
                    setDefaultValue(streamQualities[0])
                    addPreference(it)
                }
            }

        }

        companion object {
            const val CLOSE_PLAYER = "close_player_when_app_closes"
            const val AUTO_START_RADIO = "auto_start_radio"
            const val STREAM_QUALITY = "stream_quality"
            val streamQualities = arrayOf("highest", "medium", "lowest")
        }
    }
}