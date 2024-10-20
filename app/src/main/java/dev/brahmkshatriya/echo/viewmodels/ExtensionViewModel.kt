package dev.brahmkshatriya.echo.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.db.models.ExtensionEntity
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader.Companion.priorityKey
import dev.brahmkshatriya.echo.extensions.ExtensionLoader.Companion.setupMusicExtension
import dev.brahmkshatriya.echo.extensions.downloadUpdate
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.getUpdateFileUrl
import dev.brahmkshatriya.echo.extensions.installExtension
import dev.brahmkshatriya.echo.extensions.uninstallExtension
import dev.brahmkshatriya.echo.ui.common.ClientLoadingAdapter
import dev.brahmkshatriya.echo.ui.common.ClientNotSupportedAdapter
import dev.brahmkshatriya.echo.ui.extension.ClientSelectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import tel.jeelpa.plugger.utils.mapState
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val app: Application,
    val extensionLoader: ExtensionLoader,
    val messageFlow: MutableSharedFlow<SnackBar.Message>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    val extensionFlow: MutableStateFlow<MusicExtension?>,
    val settings: SharedPreferences,
    val database: EchoDatabase,
    val userFlow: MutableSharedFlow<UserEntity?>,
    val refresher: MutableSharedFlow<Boolean>
) : ClientSelectionViewModel(throwableFlow) {

    override val metadataFlow: StateFlow<List<Metadata>?> = extensionListFlow.mapState {
        it?.map { extension -> extension.metadata }
    }
    override val currentFlow: StateFlow<String?> = extensionFlow.mapState { it?.metadata?.id }

    override fun onClientSelected(clientId: String) {
        setExtension(extensionListFlow.getExtension(clientId) as MusicExtension?)
    }

    val currentExtension
        get() = extensionFlow.value

    private val userDao = database.userDao()
    fun setExtension(extension: MusicExtension?) {
        setupMusicExtension(
            viewModelScope, settings, extensionFlow, userDao, userFlow, throwableFlow, extension
        )
    }

    fun refresh() = viewModelScope.launch { refresher.emit(true) }

    private val extensionDao = database.extensionDao()
    fun setExtensionEnabled(extensionType: ExtensionType, id: String, checked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionDao.setExtension(ExtensionEntity(id, extensionType, checked))
            refresher.emit(true)
        }
    }

    fun onSettingsChanged(extension: Extension<*>, settings: Settings, key: String?) {
        viewModelScope.launch {
            extension.get<SettingsChangeListenerClient, Unit>(throwableFlow) {
                onSettingsChanged(settings, key)
            }
        }
    }

    suspend fun install(context: FragmentActivity, file: File, installAsApk: Boolean): Boolean {
        val result = installExtension(context, file, installAsApk).getOrElse {
            throwableFlow.emit(it)
            false
        }
        if (result) messageFlow.emit(SnackBar.Message(app.getString(R.string.extension_installed_successfully)))
        return result
    }

    suspend fun uninstall(
        context: FragmentActivity, extension: Extension<*>, function: (Boolean) -> Unit
    ) {
        val result = uninstallExtension(context, extension).getOrElse {
            throwableFlow.emit(it)
            false
        }
        if (result) messageFlow.emit(SnackBar.Message(app.getString(R.string.extension_uninstalled_successfully)))
        function(result)
    }

    fun moveExtensionItem(type: ExtensionType, toPos: Int, fromPos: Int) {
        val flow = extensionLoader.priorityMap[type]!!
        val list = getExtensionListFlow(type).value.orEmpty().map { it.id }.toMutableList()
        list.add(toPos, list.removeAt(fromPos))
        flow.value = list
        settings.edit {
            putString(type.priorityKey(), list.joinToString(","))
        }
    }

    private var checkedForUpdates = false
    fun updateExtensions(context: FragmentActivity) {
        if (checkedForUpdates) return
        checkedForUpdates = true
        val check = settings.getBoolean("check_for_extension_updates", true)
        if (check) viewModelScope.launch {
            ExtensionType.entries.map { type ->
                val flow = getExtensionListFlow(type)
                flow.first { it != null }!!
            }.flatten().forEach {
                updateExtension(context, it)
            }
        }
    }


    val client = OkHttpClient()
    private suspend fun updateExtension(
        context: FragmentActivity,
        extension: Extension<*>
    ) {
        val currentVersion = extension.version
        val updateUrl = extension.metadata.updateUrl ?: return

        val url = getUpdateFileUrl(currentVersion, updateUrl, client).getOrElse {
            throwableFlow.emit(it)
            null
        } ?: return

        messageFlow.emit(
            SnackBar.Message(
                app.getString(R.string.downloading_update_for_extension, extension.name)
            )
        )
        val file = downloadUpdate(context, url, client).getOrElse {
            throwableFlow.emit(it)
            null
        } ?: return
        val installAsApk = extension.metadata.importType == ImportType.App
        val result = installExtension(context, file, installAsApk).getOrElse {
            throwableFlow.emit(it)
            false
        }
        if (result) messageFlow.emit(
            SnackBar.Message(
                app.getString(R.string.extension_updated_successfully, extension.name)
            )
        )
    }

    fun getExtensionListFlow(type: ExtensionType) = when (type) {
        ExtensionType.MUSIC -> extensionListFlow
        ExtensionType.TRACKER -> trackerListFlow
        ExtensionType.LYRICS -> lyricsListFlow
    }


    companion object {
        fun Context.noClient() = SnackBar.Message(
            getString(R.string.error_no_client)
        )

        fun Context.searchNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.search), client)
        )

        fun Context.trackNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.track), client)
        )

        fun Context.radioNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.radio), client)
        )

        fun Context.loginNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.login), client)
        )

        inline fun <reified T> RecyclerView.applyAdapter(
            extension: Extension<*>?,
            name: Int,
            adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>
        ) {
            val client = extension?.instance?.value?.getOrNull()
            setAdapter(
                if (extension == null)
                    ClientLoadingAdapter()
                else if (client !is T)
                    ClientNotSupportedAdapter(name, extension.name)
                else adapter
            )
        }
    }
}