package dev.brahmkshatriya.echo.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadingFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val extensionLoader: ExtensionLoader,
    private val downloader: Downloader,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow) {

    private suspend fun add(clientId: String, item: EchoMediaItem) {
        val extension = extensionListFlow.getExtension(clientId) ?: return
        val (tracks, context) = when (item) {
            is EchoMediaItem.Lists.AlbumItem -> {
                val tracks = extension.get<AlbumClient, List<Track>>(throwableFlow) {
                    loadTracks(item.album).loadAll()
                } ?: return
                tracks to item
            }

            is EchoMediaItem.Lists.PlaylistItem -> {
                val tracks = extension.get<PlaylistClient, List<Track>>(throwableFlow) {
                    loadTracks(item.playlist).loadAll()
                } ?: return
                tracks to item
            }

            is EchoMediaItem.Lists.RadioItem -> {
                val tracks = extension.get<RadioClient, List<Track>>(throwableFlow) {
                    loadTracks(item.radio).loadAll()
                } ?: return
                tracks to item
            }

            is EchoMediaItem.TrackItem -> listOf(item.track) to null
            else -> emptyList<Track>() to null
        }
        if (tracks.isEmpty()) return
        downloader.add(clientId, context, tracks)
    }

    fun addToDownload(
        activity: FragmentActivity, clientId: String, item: EchoMediaItem
    ) = viewModelScope.launch(Dispatchers.IO) {
        with(activity) {
            messageFlow.emit(SnackBar.Message(getString(R.string.downloading_item, item.title)))
            add(clientId, item)
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

}