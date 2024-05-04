package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R

class AudioFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.audio)
    override val transitionName = "audio"
    override val creator = { AudioPreference() }

    class AudioPreference : PreferenceFragmentCompat() {
        private val equalizerActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

            }

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
                    key = KEEP_QUEUE
                    title = getString(R.string.keep_queue)
                    summary = getString(R.string.keep_queue_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

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
                    key = SKIP_SILENCE
                    title = getString(R.string.skip_silence)
                    summary = getString(R.string.skip_silence_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
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

                ListPreference(context).apply {
                    key = STREAM_QUALITY
                    title = getString(R.string.stream_quality)
                    summary = getString(R.string.stream_quality_summary)
                    entries = context.resources.getStringArray(R.array.stream_qualities)
                    entryValues = streamQualities
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(streamQualities[0])
                    addPreference(this)
                }

                Preference(context).apply {
                    key = EQUALIZER
                    title = getString(R.string.equalizer)
                    summary = getString(R.string.equalizer_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false

                    // Intent for the hidden device equalizer, might not be available on all devices
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                    val isAvailable = intent.resolveActivity(context.packageManager) != null

                    setOnPreferenceClickListener {
                        equalizerActivityLauncher.launch(intent)
                        true
                    }

                    if (isAvailable) {
                        addPreference(this)
                    }
                }
            }

        }

        companion object {
            const val KEEP_QUEUE = "keep_playlist"
            const val CLOSE_PLAYER = "close_player_when_app_closes"
            const val SKIP_SILENCE = "skip_silence"
            const val AUTO_START_RADIO = "auto_start_radio"
            const val STREAM_QUALITY = "stream_quality"
            const val EQUALIZER = "equalizer"
            val streamQualities = arrayOf("highest", "medium", "lowest")
        }
    }
}
