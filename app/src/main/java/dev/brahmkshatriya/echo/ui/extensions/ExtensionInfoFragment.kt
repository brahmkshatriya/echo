package dev.brahmkshatriya.echo.ui.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingCategory
import dev.brahmkshatriya.echo.common.settings.SettingItem
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.common.settings.SettingMultipleChoice
import dev.brahmkshatriya.echo.common.settings.SettingOnClick
import dev.brahmkshatriya.echo.common.settings.SettingSlider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.extensionPrefId
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.toSettings
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.with
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.STREAM_QUALITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.streamQualities
import dev.brahmkshatriya.echo.ui.main.settings.BaseSettingsFragment
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialTextInputPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionInfoFragment : BaseSettingsFragment() {
    private val args by lazy { requireArguments() }
    private val name by lazy { args.getString("name")!! }
    private val extensionId by lazy { args.getString("id")!! }
    private val extensionType by lazy { args.getString("type")!! }
    private val extIcon by lazy { args.getSerialized<ImageHolder>("icon") }

    private val extension by lazy {
        viewModel.extensionLoader
            .getFlow(ExtensionType.valueOf(extensionType)).getExtension(extensionId)
    }

    override val title get() = name
    override val icon get() = extIcon
    override val circleIcon = true
    override val creator = { ExtensionPreference().apply { arguments = args } }
    private val viewModel by activityViewModel<ExtensionsViewModel>()

    companion object {
        fun getBundle(
            name: String, id: String, type: ExtensionType, icon: ImageHolder?
        ) = Bundle().apply {
            putString("name", name)
            putString("id", id)
            putString("type", type.name)
            putSerialized("icon", icon)
        }

        fun getBundle(extension: Extension<*>) =
            getBundle(extension.name, extension.id, extension.type, extension.metadata.icon)

        fun Activity.openLink(url: String) {
            val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
            startActivity(intent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val extension = extension
        if (extension == null) {
            parentFragmentManager.popBackStack()
            return
        }

        super.onViewCreated(view, savedInstanceState)
        val metadata = extension.metadata
        if (metadata.importType != ImportType.BuiltIn) {
            binding.toolBar.inflateMenu(R.menu.extensions_menu)
            if (metadata.repoUrl == null) {
                binding.toolBar.menu.removeItem(R.id.menu_repo)
            }
            binding.toolBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_uninstall -> {
                        parentFragmentManager.popBackStack()
                        viewModel.uninstall(requireActivity(), extension)
                        true
                    }

                    R.id.menu_repo -> {
                        requireActivity().openLink(metadata.repoUrl!!)
                        true
                    }

                    R.id.menu_update -> {
                        viewModel.update(extension)
                        true
                    }

                    else -> false
                }
            }
        }
    }

    class ExtensionPreference : PreferenceFragmentCompat() {
        private val extensionId by lazy { arguments?.getString("id")!! }
        private val extensionType by lazy { arguments?.getString("type")!! }
        private val viewModel by activityViewModel<ExtensionsViewModel>()
        private val extension by lazy {
            viewModel.extensionLoader
                .getFlow(ExtensionType.valueOf(extensionType)).getExtension(extensionId)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = extensionPrefId(extensionType, extensionId)
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen
            val extension = extension ?: return
            val prefs = preferenceManager.sharedPreferences ?: return

            viewModel.viewModelScope.launch {
                extension.with(viewModel.app.throwFlow) {
                    val result = extension.instance.value()
                    val isLoginClient = result.getOrNull() is LoginClient
                    val infoPreference = ExtensionInfoPreference(
                        this@ExtensionPreference, extension, isLoginClient
                    )
                    screen.addPreference(infoPreference)

                    val client = result.getOrThrow()
                    if (extension.type == ExtensionType.MUSIC) MaterialListPreference(context).apply {
                        key = STREAM_QUALITY
                        title = getString(R.string.stream_quality)
                        summary = getString(R.string.x_specific_quality_summary, extension.name)
                        entries =
                            context.resources.getStringArray(R.array.stream_qualities) + getString(R.string.off)
                        entryValues = streamQualities + "off"
                        layoutResource = R.layout.preference
                        isIconSpaceReserved = false
                        setDefaultValue("off")
                        screen.addPreference(this)
                    }

                    client.settingItems.forEach { setting ->
                        setting.addPreferenceTo(screen)
                    }

                    val settings = toSettings(prefs)
                    if (this is SettingsChangeListenerClient) {
                        prefs.registerOnSharedPreferenceChangeListener { _, key ->
                            viewModel.onSettingsChanged(extension, settings, key)
                        }
                        preferenceManager.setOnPreferenceTreeClickListener {
                            viewModel.onSettingsChanged(extension, settings, it.key)
                            true
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
    }
}