package dev.brahmkshatriya.echo.viewmodels

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.models.DownloadEntity
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadItem
import dev.brahmkshatriya.echo.ui.download.DownloadItem.Companion.toItem
import dev.brahmkshatriya.echo.ui.download.DownloadingFragment
import dev.brahmkshatriya.echo.ui.paging.toFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val application: Application,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow) {

    val downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    private val visibleGroups = mutableSetOf<String>()
    private val downloadEntities = MutableStateFlow<List<DownloadEntity>>(emptyList())
    private val downloader = Downloader(extensionListFlow, database)

    init {
        viewModelScope.launch {
            downloader.dao.getDownloadsFlow().collect { list ->
                downloadEntities.value = list
                applyDownloadItems()
            }
        }
    }

    private fun applyDownloadItems() {
        val list = downloadEntities.value
        val downloadItems = mutableListOf<DownloadItem>()
        val groups = mutableMapOf<String, List<DownloadItem>>()
        list.forEach { entity ->
            val item = entity.toItem(application, extensionListFlow) ?: return@forEach
            if (entity.groupName != null) {
                groups[entity.groupName] = groups[entity.groupName].orEmpty() + item
            } else {
                downloadItems.add(item)
            }
        }
        groups.forEach { (groupName, items) ->
            downloadItems.add(DownloadItem.Group(groupName, true))
            if (visibleGroups.contains(groupName)) downloadItems.addAll(items)
        }
        downloads.value = downloadItems
    }

    fun addToDownload(
        activity: FragmentActivity, clientId: String, item: EchoMediaItem
    ) = viewModelScope.launch {
        tryWith {
            downloader.addToDownload(activity, clientId, item)
            with(activity) {
                messageFlow.emit(
                    SnackBar.Message(
                        getString(R.string.download_started),
                        SnackBar.Action(getString(R.string.view)) {
                            openFragment(DownloadingFragment())
                        }
                    )
                )
            }
        }
    }

    fun toggleGroup(name: String, checked: Boolean) {
        if (checked) visibleGroups.add(name)
        else visibleGroups.remove(name)
        applyDownloadItems()
    }

    fun toggleDownloading(download: DownloadItem.Single, downloading: Boolean) {
        viewModelScope.launch {
            if (downloading) {
                tryWith { downloader.resumeDownload(application, download.id) }
            } else {
                tryWith { downloader.pauseDownload(application, download.id) }
            }
        }
    }

    fun removeDownload(download: DownloadItem.Single) {
        viewModelScope.launch {
            tryWith { downloader.removeDownload(application, download.id) }
        }
    }

    val offlineFlow = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    fun loadOfflineDownloads() {
        offlineFlow.value = null
        viewModelScope.launch {
            val offline =
                extensionListFlow.getExtension(OfflineExtension.metadata.id)?.client as OfflineExtension
            offline.getDownloads().toFlow().collectTo(offlineFlow)
        }
    }
}