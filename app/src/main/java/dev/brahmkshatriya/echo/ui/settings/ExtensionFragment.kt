package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingCategory
import dev.brahmkshatriya.echo.common.settings.SettingItem
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.common.settings.SettingSwitch

class ExtensionFragment(private val extension: ExtensionClient) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = extension.metadata.id
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        extension.settings.forEach { setting ->
            setting.addPreferenceTo(screen)
        }
    }

    private fun Setting.addPreferenceTo(preferenceGroup: PreferenceGroup) {
        when (this) {
            is SettingCategory -> {
                PreferenceCategory(preferenceGroup.context).also {
                    it.title = this.title
                    it.key = this.key

                    it.isIconSpaceReserved = false
                    it.layoutResource = R.layout.preference_category
                    preferenceGroup.addPreference(it)

                    this.items.forEach { item ->
                        item.addPreferenceTo(it)
                    }
                }
            }

            is SettingItem -> {
                Preference(preferenceGroup.context).also {
                    it.title = this.title
                    it.key = this.key
                    it.summary = this.summary

                    it.isIconSpaceReserved = false
                    it.layoutResource = R.layout.preference
                    preferenceGroup.addPreference(it)
                }
            }

            is SettingSwitch -> {
                SwitchPreference(preferenceGroup.context).also {
                    it.title = this.title
                    it.key = this.key
                    it.summary = this.summary
                    it.setDefaultValue(this.defaultValue)

                    it.isIconSpaceReserved = false
                    it.layoutResource = R.layout.preference_switch
                    preferenceGroup.addPreference(it)
                }
            }

            is SettingList -> {
                ListPreference(preferenceGroup.context).also {
                    it.title = this.title
                    it.key = this.key
                    defaultEntryIndex?.let { index ->
                        it.setDefaultValue(this.entryValues[index])
                        it.summary = this.entryTitles[index]
                    }
                    it.entries = this.entryTitles.toTypedArray()
                    it.entryValues = this.entryValues.toTypedArray()

                    it.isIconSpaceReserved = false
                    it.layoutResource = R.layout.preference
                    preferenceGroup.addPreference(it)
                }
            }

            else -> throw IllegalArgumentException("Unsupported setting type")
        }
    }
}