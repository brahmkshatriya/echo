package dev.brahmkshatriya.echo.ui.download

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.withExtensionId
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.extensions.add.ExtensionsAddListBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadViewModel(
    app: App,
    extensionLoader: ExtensionLoader,
    private val downloader: Downloader,
) : ViewModel() {

    val downloaded = MutableStateFlow(PagingUtils.Data<Shelf>(null, null, null, null))
    private val context = app.context
    private val messageFlow = app.messageFlow
    private val throwableFlow = app.throwFlow
    private val downloadList = extensionLoader.extensions.misc

    val extensions = extensionLoader.extensions
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

    fun cancel(trackId: Long) {
        downloader.cancel(trackId)
    }

    fun restart(trackId: Long) {
        downloader.restart(trackId)
    }

    fun cancelAll() {
        downloader.cancelAll()
    }

    fun deleteDownload(item: EchoMediaItem) {
        when(item) {
            is EchoMediaItem.TrackItem -> downloader.deleteDownload(item.id)
            else -> downloader.deleteContext(item.id)
        }
        viewModelScope.launch {
            messageFlow.emit(
                Message(context.getString(R.string.removed_x_from_downloads, item.title))
            )
        }
    }

    init {
        viewModelScope.launch {
            flow.collectLatest { infos ->
                val downloads = infos.filter { it.download.finalFile != null }.groupBy {
                    it.context?.id
                }.flatMap { (id, infos) ->
                    if (id == null) infos.map {
                        it.download.track.toMediaItem().toShelf()
                            .withExtensionId(it.download.extensionId)
                    }
                    else listOf(infos.first().run {
                        context!!.mediaItem.toShelf().withExtensionId(download.extensionId)
                    })
                }
                val extension = extensions.music.getExtensionOrThrow(UnifiedExtension.metadata.id)
                downloaded.value = PagingUtils.Data(
                    extension,
                    "downloaded",
                    PagedData.Single { downloads },
                    PagingUtils.from(downloads)
                )
            }
        }
    }
}