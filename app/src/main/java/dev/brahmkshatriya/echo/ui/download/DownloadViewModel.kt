package dev.brahmkshatriya.echo.ui.download

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.add.ExtensionsAddListBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadViewModel(
    app: App,
    extensionLoader: ExtensionLoader,
    private val downloader: Downloader,
) : ViewModel() {

    private val context = app.context
    private val messageFlow = app.messageFlow
    private val throwableFlow = app.throwFlow
    private val downloadList = extensionLoader.extensions.misc

    val flow = downloader.flow

    fun addToDownload(
        activity: FragmentActivity, clientId: String, item: EchoMediaItem
    ) = viewModelScope.launch(Dispatchers.IO) {
        with(activity) {
            messageFlow.emit(Message(getString(R.string.downloading_x, item.title)))
            val downloadExt = downloadList.value?.firstOrNull {
                it.metadata.isEnabled && it.isClient<DownloadClient>()
            } ?: return@with messageFlow.emit(
                Message(
                    context.getString(R.string.no_download_extension),
                    Message.Action(getString(R.string.add_extension)) {
                        ExtensionsAddListBottomSheet.LinkFile()
                            .show(supportFragmentManager, null)
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
                        openFragment<DownloadFragment>()
                    }
                )
            )
        }
    }
}