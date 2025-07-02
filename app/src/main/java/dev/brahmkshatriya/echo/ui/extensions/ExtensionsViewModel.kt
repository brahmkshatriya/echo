package dev.brahmkshatriya.echo.ui.extensions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.with
import dev.brahmkshatriya.echo.extensions.InstallationUtils.installApp
import dev.brahmkshatriya.echo.extensions.InstallationUtils.installFile
import dev.brahmkshatriya.echo.extensions.InstallationUtils.uninstallApp
import dev.brahmkshatriya.echo.extensions.InstallationUtils.uninstallFile
import dev.brahmkshatriya.echo.extensions.db.models.ExtensionEntity
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInstallerBottomSheet.Companion.createLinksDialog
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionListViewModel
import dev.brahmkshatriya.echo.utils.AppUpdater.downloadUpdate
import dev.brahmkshatriya.echo.utils.AppUpdater.getUpdateFileUrl
import dev.brahmkshatriya.echo.utils.AppUpdater.updateApp
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.ContextUtils.cleanupTempApks
import dev.brahmkshatriya.echo.utils.ContextUtils.collect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class ExtensionsViewModel(
    val extensionLoader: ExtensionLoader,
    val app: App
) : ExtensionListViewModel<MusicExtension>() {
    override val extensionsFlow = extensionLoader.music
    override val currentSelectionFlow = extensionLoader.current
    override fun onExtensionSelected(extension: MusicExtension) {
        extensionLoader.setupMusicExtension(extension, true)
    }

    fun onSettingsChanged(extension: Extension<*>, settings: Settings, key: String?) {
        viewModelScope.launch {
            extension.get<SettingsChangeListenerClient, Unit>(app.throwFlow) {
                onSettingsChanged(settings, key)
            }
        }
    }

    private val extensionDao = extensionLoader.db.extensionDao()
    fun setExtensionEnabled(extensionType: ExtensionType, id: String, checked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionDao.setExtension(ExtensionEntity(id, extensionType, checked))
        }
    }

    fun changeExtension(id: String) {
        viewModelScope.launch {
            runCatching {
                val ext = extensionLoader.music.getExtensionOrThrow(id)
                extensionLoader.setupMusicExtension(ext, true)
            }.getOrElse {
                app.throwFlow.emit(it)
            }
        }
    }

    val lastSelectedManageExt = MutableStateFlow(0)
    val manageExtListFlow = extensionLoader.all.combine(lastSelectedManageExt) { _, last ->
        extensionLoader.getFlow(ExtensionType.entries[last]).value
    }

    fun moveExtensionItem(toPos: Int, fromPos: Int) {
        val type = ExtensionType.entries[lastSelectedManageExt.value]
        val flow = extensionLoader.priorityMap[type]!!
        val list = extensionLoader.getFlow(type).value.map { it.id }.toMutableList()
        list.add(toPos, list.removeAt(fromPos))
        flow.value = list
    }

    private val updateTime = 1000 * 60 * 60 * 2 // Check every 2hrs
    private fun shouldCheckForExtensionUpdates(): Boolean {
        val check = app.settings.getBoolean("check_for_extension_updates", true)
        if (!check) return false
        val lastUpdateCheck = app.context.getFromCache<Long>("last_update_check") ?: 0
        return System.currentTimeMillis() - lastUpdateCheck > updateTime
    }

    private suspend fun message(msg: String) {
        app.messageFlow.emit(Message(msg))
    }

    fun update(activity: FragmentActivity, force: Boolean) = viewModelScope.launch {
        if (!force && !shouldCheckForExtensionUpdates()) return@launch
        activity.saveToCache("last_update_check", System.currentTimeMillis())
        activity.cleanupTempApks()
        message(app.context.getString(R.string.checking_for_extension_updates))
        val appApk = updateApp(app)
        runCatching {
            if (appApk != null) {
                activity.saveToCache("last_update_check", 0)
                awaitInstallation(appApk).getOrThrow()
            } else extensionLoader.all.value.forEach { updateExt(it) }
        }.getOrElse { app.throwFlow.emit(it) }
    }

    data class PromptResult(
        val file: File,
        val accepted: Boolean,
        val type: ImportType,
        val id: String,
        val supportedLinks: List<String>
    )

    val installPromptFlow = MutableSharedFlow<File>()
    private val promptResultFlow = MutableSharedFlow<PromptResult>()
    val installFileFlow = MutableSharedFlow<File>()
    val installedFlow = MutableSharedFlow<Pair<File, Result<Unit>>>()
    val linksDialogFlow = MutableSharedFlow<Pair<File, List<String>>>()

    private suspend fun install(id: String, type: ImportType, file: File): Result<Unit> {
        return if (type == ImportType.App) awaitInstallation(file)
        else runCatching { installFile(app.context, extensionLoader.fileIgnoreFlow, id, file) }
    }

    private suspend fun awaitInstallation(file: File): Result<Unit> {
        installFileFlow.emit(file)
        return installedFlow.first { it.first == file }.second
    }

    fun promptDismissed(
        file: File, install: Boolean, type: ImportType, id: String, supportedLinks: List<String>
    ) = viewModelScope.launch {
        promptResultFlow.emit(PromptResult(file, install, type, id, supportedLinks))
    }

    private suspend fun updateExt(ext: Extension<*>, show: Boolean = false) {
        val file = getExtensionUpdate(ext, show) ?: return
        install(ext.id, ext.metadata.importType, file).onFailure {
            app.throwFlow.emit(it)
            return
        }
        message(app.context.getString(R.string.extension_updated_successfully, ext.name))
    }

    fun update(extension: Extension<*>) = viewModelScope.launch { updateExt(extension, true) }

    fun installWithPrompt(files: List<File>) = viewModelScope.launch {
        files.forEach { file ->
            installPromptFlow.emit(file)
            val result = promptResultFlow.first { it.file == file }
            if (!result.accepted) return@forEach
            install(result.id, result.type, result.file).onFailure {
                app.throwFlow.emit(it)
                return@forEach
            }
            message(app.context.getString(R.string.extension_installed_successfully))
            if (result.type == ImportType.App)
                linksDialogFlow.emit(file to result.supportedLinks)
        }
    }

    fun uninstall(activity: FragmentActivity, extension: Extension<*>) = viewModelScope.launch {
        val fileResult = runCatching {
            uninstallFile(extensionLoader.fileIgnoreFlow, extension.metadata.path)
        }.exceptionOrNull()
        val appResult = runCatching {
            uninstallApp(activity, extension.metadata.path)
        }.exceptionOrNull()
        val result = if (extension.metadata.importType == ImportType.App) appResult else fileResult
        if (result == null) message(app.context.getString(R.string.extension_uninstalled_successfully))
        else app.throwFlow.emit(result)
    }

    companion object {
        fun FragmentActivity.configureExtensionsUpdater() {
            val viewModel by viewModel<ExtensionsViewModel>()
            collect(viewModel.installPromptFlow) {
                ExtensionInstallerBottomSheet.newInstance(it).show(supportFragmentManager, null)
            }
            collect(viewModel.linksDialogFlow) {
                createLinksDialog(it.first, it.second)
            }

            viewModel.update(this, false)
            var currentFile: File? = null
            collect(viewModel.installFileFlow) {
                currentFile = it
                viewModel.installedFlow.emit(it to runCatching { installApp(this, it) })
            }
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    val file = currentFile ?: return
                    viewModel.run {
                        viewModelScope.launch {
                            installedFlow.emit(
                                file to Result.failure(CancellationException())
                            )
                        }
                    }
                }
            })
        }
    }

    private val client = OkHttpClient()
    private suspend fun getExtensionUpdate(
        extension: Extension<*>,
        show: Boolean = false
    ): File? {
        val currentVersion = extension.version
        val updateUrl = extension.metadata.updateUrl ?: return null
        val url = extension.with(app.throwFlow) {
            getUpdateFileUrl(currentVersion, updateUrl, client).getOrThrow()
        }
        if (url == null) {
            if (show) message(
                app.context.getString(R.string.no_update_available_for_x, extension.name)
            )
            return null
        }
        message(app.context.getString(R.string.downloading_update_for_x, extension.name))
        val file = extension.with(app.throwFlow) {
            downloadUpdate(app.context, url, client).getOrThrow()
        } ?: return null
        return file
    }


}