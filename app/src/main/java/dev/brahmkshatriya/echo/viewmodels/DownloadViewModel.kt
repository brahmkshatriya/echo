package dev.brahmkshatriya.echo.viewmodels

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadingFragment
import dev.brahmkshatriya.echo.ui.extension.ExtensionsAddListBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val downloadList: MutableStateFlow<List<MiscExtension>?>,
    private val extensionLoader: ExtensionLoader,
    private val downloader: Downloader,
    private val context: Application,
    private val messageFlow: MutableSharedFlow<Message>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow) {

    fun addToDownload(
        activity: FragmentActivity, clientId: String, item: EchoMediaItem
    ) = viewModelScope.launch(Dispatchers.IO) {
        with(activity) {
            messageFlow.emit(Message(getString(R.string.downloading_item, item.title)))
            val downloadExt = downloadList.value?.firstOrNull {
                it.metadata.enabled && it.isClient<DownloadClient>()
            }
            downloadExt ?: return@with messageFlow.emit(
                Message(
                    context.getString(R.string.no_download_extension),
                    Message.Action(getString(R.string.add_extension)) {
                        ExtensionsAddListBottomSheet.LinkFile().show(supportFragmentManager, null)
                    }
                )
            )

            val downloads = downloadExt.get<DownloadClient, List<DownloadContext>>(throwableFlow) {
                getDownloadTracks(clientId, item)
            } ?: return@with

            if (downloads.isEmpty()) return@with messageFlow.emit(
                Message(context.getString(R.string.nothing_to_download_in_x, item.title))
            )

            downloader.add(downloads)
            messageFlow.emit(
                Message(
                    getString(R.string.download_started),
                    Message.Action(getString(R.string.view)) {
                        openFragment(DownloadingFragment())
                    }
                )
            )
        }
    }

    fun pauseDownload(downloadIds: List<Long>) {
        downloader.pause(downloadIds)
    }

    fun resumeDownload(downloadIds: List<Long>) {
        downloader.resume(downloadIds)
    }

    fun cancelDownload(downloadIds: List<Long>) {
        downloader.cancel(downloadIds)
    }

    val downloadsFlow = downloader.downloadsFlow
    val dao = downloader.dao

    fun getOfflineDownloads() = extensionLoader.offline.getDownloads()
    fun cancelTrackDownload(trackId: Long) {
        downloader.cancelTrackDownload(listOf(trackId))
    }

}