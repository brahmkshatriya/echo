package dev.brahmkshatriya.echo.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.ExtensionOpenerActivity
import dev.brahmkshatriya.echo.ExtensionOpenerActivity.Companion.installExtension
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.db.models.ExtensionEntity
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.ExtensionAssetResponse
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader.Companion.priorityKey
import dev.brahmkshatriya.echo.extensions.ExtensionLoader.Companion.setupMusicExtension
import dev.brahmkshatriya.echo.extensions.downloadUpdate
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.getExtensionList
import dev.brahmkshatriya.echo.extensions.getUpdateFileUrl
import dev.brahmkshatriya.echo.extensions.installExtension
import dev.brahmkshatriya.echo.extensions.uninstallExtension
import dev.brahmkshatriya.echo.extensions.waitForResult
import dev.brahmkshatriya.echo.extensions.with
import dev.brahmkshatriya.echo.ui.common.ClientLoadingAdapter
import dev.brahmkshatriya.echo.ui.common.ClientNotSupportedAdapter
import dev.brahmkshatriya.echo.ui.extension.ClientSelectionViewModel
import dev.brahmkshatriya.echo.ui.settings.ExtensionFragment.ExtensionPreference.Companion.prefId
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
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
    val messageFlow: MutableSharedFlow<Message>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    val miscListFlow: MutableStateFlow<List<MiscExtension>?>,
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

    fun setExtension(extension: MusicExtension?) {
        setupMusicExtension(
            viewModelScope, settings, extensionFlow, throwableFlow, extension
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
        if (result) messageFlow.emit(Message(app.getString(R.string.extension_installed_successfully)))
        return result
    }

    suspend fun uninstall(
        context: FragmentActivity, extension: Extension<*>, function: (Boolean) -> Unit
    ) {
        context.deleteSharedPreferences(extension.prefId)
        val result = uninstallExtension(context, extension).getOrElse {
            throwableFlow.emit(it)
            false
        }
        if (result) messageFlow.emit(Message(app.getString(R.string.extension_uninstalled_successfully)))
        function(result)
    }

    fun getExtensionListFlow(type: ExtensionType) = when (type) {
        ExtensionType.MUSIC -> extensionListFlow
        ExtensionType.TRACKER -> trackerListFlow
        ExtensionType.LYRICS -> lyricsListFlow
        ExtensionType.MISC -> miscListFlow
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

    suspend fun allExtensions() = ExtensionType.entries.map { type ->
        val flow = getExtensionListFlow(type)
        flow.first { it != null }!!
    }.flatten()

    private val updateTime = 1000 * 60 * 60 * 6
    private fun shouldCheckForUpdates(): Boolean {
        val check = settings.getBoolean("check_for_extension_updates", true)
        if (!check) return false
        val lastUpdateCheck = app.getFromCache<Long>("last_update_check") ?: 0
        return System.currentTimeMillis() - lastUpdateCheck > updateTime
    }

    fun updateExtensions(context: FragmentActivity, force: Boolean) {
        if (!shouldCheckForUpdates() && !force) return
        app.saveToCache("last_update_check", System.currentTimeMillis())
        viewModelScope.launch {
            messageFlow.emit(Message(app.getString(R.string.checking_for_extension_updates)))
            allExtensions().forEach {
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

        val url = extension.with(throwableFlow) {
            getUpdateFileUrl(currentVersion, updateUrl, client).getOrThrow()
        } ?: return

        messageFlow.emit(
            Message(
                app.getString(R.string.downloading_update_for_extension, extension.name)
            )
        )
        val file = extension.with(throwableFlow) {
            downloadUpdate(context, url, client).getOrThrow()
        } ?: return
        val installAsApk = extension.metadata.importType == ImportType.App
        val result = installExtension(context, file, installAsApk).getOrElse {
            throwableFlow.emit(it)
            false
        }
        if (result) messageFlow.emit(
            Message(
                app.getString(R.string.extension_updated_successfully, extension.name)
            )
        )
    }


    companion object {
        fun Context.noClient() = Message(
            getString(R.string.error_no_client)
        )

        fun Context.searchNotSupported(client: String) = Message(
            getString(R.string.is_not_supported, getString(R.string.search), client)
        )

        fun Context.trackNotSupported(client: String) = Message(
            getString(R.string.is_not_supported, getString(R.string.track), client)
        )

        fun Context.radioNotSupported(client: String) = Message(
            getString(R.string.is_not_supported, getString(R.string.radio), client)
        )

        fun Context.loginNotSupported(client: String) = Message(
            getString(R.string.is_not_supported, getString(R.string.login), client)
        )

        suspend inline fun <reified T> RecyclerView.applyAdapter(
            extension: Extension<*>?,
            name: Int,
            adapter: RecyclerView.Adapter<*>
        ) {
            val client = extension?.instance?.value()?.getOrNull()
            setAdapter(
                if (extension == null)
                    ClientLoadingAdapter()
                else if (client !is T)
                    ClientNotSupportedAdapter(name, extension.name)
                else adapter
            )
        }
    }

    val addingFlow = MutableStateFlow<AddState>(AddState.Init)

    sealed class AddState {
        data object Init : AddState()
        data object Loading : AddState()
        data class AddList(val list: List<ExtensionAssetResponse>?) : AddState()
    }

    fun addFromLinkOrCode(link: String) {
        viewModelScope.launch {
            addingFlow.value = AddState.Loading
            val actualLink = when {
                link.startsWith("http://") or link.startsWith("https://") -> link
                else -> "https://v.gd/$link"
            }

            val list = runCatching { getExtensionList(actualLink, client) }.getOrElse {
                throwableFlow.emit(it)
                null
            }
            addingFlow.value = AddState.AddList(list)
        }
    }

    fun addFromFile(context: FragmentActivity) {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            val result = context.waitForResult(intent)
            val file = result.data?.data ?: return@launch
            val newIntent = Intent(context, ExtensionOpenerActivity::class.java).apply {
                setData(file)
            }
            context.startActivity(newIntent)
        }
    }

    fun addExtensions(context: FragmentActivity, extensions: List<ExtensionAssetResponse>) {
        viewModelScope.launch {
            extensions.forEach { extension ->
                val url = getUpdateFileUrl("", extension.updateUrl, client).getOrElse {
                    throwableFlow.emit(it)
                    null
                } ?: return@forEach
                messageFlow.emit(
                    Message(
                        app.getString(R.string.downloading_update_for_extension, extension.name)
                    )
                )
                val file = downloadUpdate(context, url, client).getOrElse {
                    throwableFlow.emit(it)
                    return@forEach
                }
                context.installExtension(file.toUri().toString())
            }
        }
    }

}