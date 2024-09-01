package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingCategory
import dev.brahmkshatriya.echo.common.settings.SettingItem
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.common.settings.SettingMultipleChoice
import dev.brahmkshatriya.echo.common.settings.SettingSlider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.utils.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialTextInputPreference
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

class ExtensionFragment : BaseSettingsFragment() {
    private val args by lazy { requireArguments() }
    private val name by lazy { args.getString("name")!! }
    private val id by lazy { args.getString("id")!! }
    private val type by lazy { args.getString("type")!! }
    override val title get() = getString(R.string.name_settings, name)
    override val transitionName get() = id
    override val creator = { ExtensionPreference.newInstance(id, type) }

    companion object {
        fun newInstance(name: String, id: String, type: ExtensionType): ExtensionFragment {
            val bundle = Bundle().apply {
                putString("name", name)
                putString("id", id)
                putString("type", type.name)
            }
            return ExtensionFragment().apply {
                arguments = bundle
            }
        }

        fun newInstance(
            extensionMetadata: ExtensionMetadata,
            extensionType: ExtensionType
        ) = newInstance(extensionMetadata.name, extensionMetadata.id, extensionType)

        fun newInstance(extension: MusicExtension) =
            newInstance(extension.metadata, ExtensionType.MUSIC)

        fun newInstance(extension: TrackerExtension) =
            newInstance(extension.metadata, ExtensionType.TRACKER)

        fun newInstance(extension: LyricsExtension) =
            newInstance(extension.metadata, ExtensionType.LYRICS)
    }

    class ExtensionPreference : PreferenceFragmentCompat() {

        private val args by lazy { requireArguments() }
        private val extensionId by lazy { args.getString("extensionId")!! }
        private val extensionType: ExtensionType by lazy {
            val type = args.getString("extensionType")!!
            ExtensionType.valueOf(type)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = "$extensionType-$extensionId"
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            val viewModel by activityViewModels<ExtensionViewModel>()
            val client = viewModel.run {
                when (extensionType) {
                    ExtensionType.MUSIC -> extensionListFlow.getExtension(extensionId)?.client
                    ExtensionType.TRACKER -> trackerListFlow.getExtension(extensionId)?.client
                    ExtensionType.LYRICS -> lyricsListFlow.getExtension(extensionId)?.client
                }
            }

            client ?: return
            client.settingItems.forEach { setting ->
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
                    SwitchPreferenceCompat(preferenceGroup.context).also {
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
                    MaterialListPreference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
                        defaultEntryIndex?.let { index ->
                            it.setDefaultValue(this.entryValues[index])
                        }
                        it.entries = this.entryTitles.toTypedArray()
                        it.entryValues = this.entryValues.toTypedArray()

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingMultipleChoice -> {
                    MaterialMultipleChoicePreference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        defaultEntryIndices?.let { indices ->
                            it.setDefaultValue(indices.mapNotNull { index ->
                                entryValues.getOrNull(index)
                            }.toSet())
                        }
                        it.entries = this.entryTitles.toTypedArray()
                        it.entryValues = this.entryValues.toTypedArray()

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingTextInput -> {
                    MaterialTextInputPreference(preferenceGroup.context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
                        it.text = this.defaultValue

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingSlider -> {
                    MaterialSliderPreference(preferenceGroup.context, from, to, steps).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary

                        it.isIconSpaceReserved = false
                        preferenceGroup.addPreference(it)
                    }
                }

                else -> throw IllegalArgumentException("Unsupported setting type")
            }
        }

        companion object {
            fun newInstance(id: String, type: String): ExtensionPreference {
                val bundle = Bundle().apply {
                    putString("extensionId", id)
                    putString("extensionType", type)
                }
                return ExtensionPreference().apply {
                    arguments = bundle
                }
            }
        }
    }
}