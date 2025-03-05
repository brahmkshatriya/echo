package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingCategory
import dev.brahmkshatriya.echo.common.settings.SettingItem
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.common.settings.SettingMultipleChoice
import dev.brahmkshatriya.echo.common.settings.SettingOnClick
import dev.brahmkshatriya.echo.common.settings.SettingSlider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.run
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.SettingsUtils.toSettings
import dev.brahmkshatriya.echo.utils.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.prefs.MaterialTextInputPreference
import dev.brahmkshatriya.echo.utils.prefs.TransitionPreference
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionFragment : BaseSettingsFragment() {
    private val args by lazy { requireArguments() }
    private val name by lazy { args.getString("name")!! }
    private val id by lazy { args.getString("id")!! }
    private val type by lazy { args.getString("type")!! }
    override val title get() = getString(R.string.settings)
    override val creator = { ExtensionPreference.newInstance(id, type) }

    companion object {
        fun getBundle(name: String, id: String, type: ExtensionType) = Bundle().apply {
            putString("name", name)
            putString("id", id)
            putString("type", type.name)
        }

        fun getBundle(extension: Extension<*>) =
            getBundle(extension.name, extension.id, extension.type)
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
            preferenceManager.sharedPreferencesName = extensionPrefId(extensionType, extensionId)
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            val viewModel by activityViewModel<ExtensionsViewModel>()
            viewModel.apply {
                viewModelScope.launch {
                    val extension = extensions.all.getExtension(extensionId)
                    extension?.run(app.throwFlow) {
                        settingItems.forEach { setting ->
                            setting.addPreferenceTo(screen)
                        }
                        val prefs = preferenceManager.sharedPreferences ?: return@run
                        val settings = toSettings(prefs)
                        if (this is SettingsChangeListenerClient) {
                            prefs.registerOnSharedPreferenceChangeListener { _, key ->
                                onSettingsChanged(extension, settings, key)
                            }
                            preferenceManager.setOnPreferenceTreeClickListener {
                                onSettingsChanged(extension, settings, it.key)
                                true
                            }
                        }
                    }
                }
            }
        }

        private fun Setting.addPreferenceTo(preferenceGroup: PreferenceGroup) {
            val context = preferenceGroup.context
            when (this) {
                is SettingCategory -> {
                    PreferenceCategory(context).also {
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
                    Preference(context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary

                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingSwitch -> {
                    SwitchPreferenceCompat(context).also {
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
                    MaterialListPreference(context).also {
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
                    MaterialMultipleChoicePreference(context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
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
                    MaterialTextInputPreference(context).also {
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
                    MaterialSliderPreference(context, from, to, steps, allowOverride).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
                        it.setDefaultValue(this.defaultValue)

                        it.isIconSpaceReserved = false
                        preferenceGroup.addPreference(it)
                    }
                }

                is SettingOnClick -> {
                    TransitionPreference(context).also {
                        it.title = this.title
                        it.key = this.key
                        it.summary = this.summary
                        it.setOnPreferenceClickListener { runCatching { onClick() }; true }
                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }
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

            val Extension<*>.prefId get() = extensionPrefId(type, id)
            fun extensionPrefId(extensionType: ExtensionType, extensionId: String) =
                "$extensionType-$extensionId"
        }
    }
}