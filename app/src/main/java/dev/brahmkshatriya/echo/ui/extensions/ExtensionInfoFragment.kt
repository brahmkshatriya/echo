package dev.brahmkshatriya.echo.ui.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.iterator
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImportType
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
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.toSettings
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.STREAM_QUALITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.streamQualities
import dev.brahmkshatriya.echo.ui.extensions.export.ExportPlaylistUtils.Companion.deserializePlaylists
import dev.brahmkshatriya.echo.ui.extensions.export.ExportPlaylistUtils.Companion.serializePlaylists
import dev.brahmkshatriya.echo.ui.extensions.export.PickerActivity
import dev.brahmkshatriya.echo.ui.settings.BaseSettingsFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.prefs.ClickPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.LoadingPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialTextInputPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ExtensionInfoFragment : BaseSettingsFragment() {
    private val args by lazy { requireArguments() }
    private val name by lazy { args.getString("name")!! }
    private val extensionId by lazy { args.getString("id")!! }
    private val extensionType by lazy { args.getString("type")!! }
    private val extIcon by lazy { args.getSerialized<ImageHolder>("icon") }

    private val viewModel by viewModel<ExtensionInfoViewModel> {
        parametersOf(ExtensionType.valueOf(extensionType), extensionId)
    }
    private val extensionsViewModel by activityViewModel<ExtensionsViewModel>()

    override val title get() = name
    override val icon get() = extIcon
    override val creator = { ExtensionPreference().apply { arguments = args } }

    companion object {
        fun getBundle(
            name: String, id: String, type: ExtensionType, icon: ImageHolder?,
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
            runCatching { startActivity(intent) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(viewModel.stateFlow) { state ->
            val extension = state?.extension ?: return@observe
            val metadata = extension.metadata
            binding.toolBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_uninstall -> {
                        parentFragmentManager.popBackStack()
                        extensionsViewModel.uninstall(requireActivity(), extension)
                        true
                    }

                    R.id.menu_repo -> {
                        requireActivity().openLink(metadata.repoUrl!!)
                        true
                    }

                    R.id.menu_update -> {
                        extensionsViewModel.update(extension)
                        true
                    }

                    else -> false
                }
            }
            if (metadata.importType != ImportType.BuiltIn) {
                binding.toolBar.menu.run {
                    iterator().forEach { removeItem(it.itemId) }
                }
                binding.toolBar.inflateMenu(R.menu.extensions_menu)
                if (metadata.repoUrl == null) binding.toolBar.menu.removeItem(R.id.menu_repo)
            }
        }
    }

    class ExtensionPreference : PreferenceFragmentCompat() {
        private val extensionId by lazy { arguments?.getString("id")!! }
        private val extensionType by lazy { arguments?.getString("type")!! }
        private val viewModel by lazy {
            requireParentFragment().viewModel<ExtensionInfoViewModel> {
                parametersOf(ExtensionType.valueOf(extensionType), extensionId)
            }.value
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = extensionPrefId(extensionType, extensionId)
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val prefs = preferenceManager.sharedPreferences ?: return
            val settings = toSettings(prefs)
            prefs.registerOnSharedPreferenceChangeListener { _, key ->
                viewModel.onSettingsChanged(settings, key)
            }
            preferenceManager.setOnPreferenceTreeClickListener {
                viewModel.onSettingsChanged(settings, it.key)
                true
            }
            observe(viewModel.stateFlow) { state ->
                val extension = state?.extension
                val screen = preferenceManager.createPreferenceScreen(context)
                preferenceScreen = screen
                if (extension == null) {
                    screen.addPreference(LoadingPreference(context))
                    return@observe
                }
                val infoPreference = ExtensionInfoPreference(
                    this@ExtensionPreference, extension, state.isLoginClient
                )
                screen.addPreference(infoPreference)

                val client = extension.instance.value().getOrThrow()
                if (extension.id == UnifiedExtension.metadata.id) {
                    ClickPreference(context).apply {
                        key = "export_playlists"
                        title = getString(R.string.export_playlists)
                        summary = getString(R.string.export_playlists_summary)
                        layoutResource = R.layout.preference
                        isIconSpaceReserved = false
                        setOnClickListener {
                            exportPlaylists(client)
                        }
                        screen.addPreference(this)
                    }
                    ClickPreference(context).apply {
                        key = "import_playlists"
                        title = getString(R.string.import_playlists)
                        summary = getString(R.string.import_playlists_summary)
                        layoutResource = R.layout.preference
                        isIconSpaceReserved = false
                        setOnClickListener {
                            importPlaylists(client)
                        }
                        screen.addPreference(this)
                    }
                }
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
                state.settings.forEach { it.addPreferenceTo(screen) }

            }
        }

        private fun exportPlaylists(client: ExtensionClient) {
            val context = preferenceManager.context
            if (client is PlaylistEditClient) {
                PickerActivity.openFolder(context) { uri ->
                    val directory = DocumentFile.fromTreeUri(context, uri) ?: run {
                        throw Exception("Failed to get directory from URI")
                    }

                    val file = directory.createFile("application/json", "playlists") ?: run {
                        throw Exception("Failed to create file")
                    }

                    viewModel.viewModelScope.launch {
                        try {
                            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                                outputStream.write(client.serializePlaylists().toByteArray())
                            }
                        } catch (e: Exception) {
                            file.delete()
                            throw e
                        }
                    }
                }
            }
        }

        private fun importPlaylists(client: ExtensionClient) {
            val context = preferenceManager.context
            if (client is PlaylistEditClient) {
                PickerActivity.openFile(context) { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { reader ->
                            val text = reader.readText()
                            viewModel.viewModelScope.launch {
                                client.deserializePlaylists(text)
                            }
                        }
                    } ?: throw Exception("Cannot open input stream for $uri")
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
                        it.setOnPreferenceClickListener {
                            viewModel.onSettingsClick(onClick)
                            true
                        }
                        it.isIconSpaceReserved = false
                        it.layoutResource = R.layout.preference
                        preferenceGroup.addPreference(it)
                    }
                }
            }
        }
    }
}