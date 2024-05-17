package dev.brahmkshatriya.echo.ui.download

import android.content.Context
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.models.DownloadEntity
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.utils.getFromCache
import kotlinx.coroutines.flow.MutableStateFlow

sealed class DownloadItem {
    data class Single(
        val id: Long,
        val item: EchoMediaItem,
        val clientId: String,
        val clientIcon: String?,
        val progress: Int,
        val isDownloading: Boolean,
        val groupName: String? = null,
    ) : DownloadItem()

    data class Group(
        val name: String,
        val areChildrenVisible: Boolean
    ) : DownloadItem()

    companion object {
        fun DownloadEntity.toItem(
            context: Context, extensionList: MutableStateFlow<List<MusicExtension>?>
        ): DownloadItem? {
            val extension = extensionList.getExtension(clientId) ?: return null
            val item = context.getFromCache(itemId, Track.creator, "downloads")?.toMediaItem()
                ?: return null
            return DownloadItem.Single(
                id = id,
                item = item,
                clientId = clientId,
                clientIcon = extension.metadata.iconUrl,
                progress = 0,
                isDownloading = false,
                groupName = groupName
            )
        }
    }
}