package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Streamable
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * The client for downloading tracks. Needs to support the following:
 * - Get the download tracks for the given media item.
 * - Get the download folder for the given context of downloading track.
 * - Which server to use for downloading.
 * - Which sources to use for downloading.
 * - Download the given sources.
 * - Merge the given media files into a single file.
 * - Tag a file with the given track metadata.
 * - The maximum number of concurrent downloads allowed.
 */
interface DownloadClient : ExtensionClient {

    /**
     * Get the download tracks for the given [item].
     *
     * @param extensionId The client ID.
     * @param item The media item to get the download tracks for.
     *
     * @return The download tracks.
     *
     * @see DownloadContext
     * @see EchoMediaItem
     */
    suspend fun getDownloadTracks(extensionId: String, item: EchoMediaItem): List<DownloadContext>

    /**
     * The maximum number of concurrent downloads allowed.
     */
    val concurrentDownloads: Int

    /**
     * Which server to use for downloading.
     * use this to get the available servers.
     * ```
     * val servers = context.track.servers
     * ```
     * @param context The context of the downloading track.
     *
     * @see Streamable
     */
    suspend fun selectServer(context: DownloadContext): Streamable

    /**
     * Which source to use for downloading.
     *
     * @param context The context of the downloading track.
     * @param server The server to choose sources from.
     *
     * @return The selected sources.
     *
     * @see Streamable.Media.Server
     * @see Streamable.Source
     */
    suspend fun selectSources(
        context: DownloadContext, server: Streamable.Media.Server
    ): List<Streamable.Source>

    /**
     * Download the given [source].
     *
     * @param progressFlow The flow to emit the download progress.
     * @param context The context of the downloading track.
     * @param source The source to download.
     */
    suspend fun download(
        progressFlow: MutableStateFlow<Progress>,
        context: DownloadContext,
        source: Streamable.Source
    ): File

    /**
     * Merge the given media [files] into a single file.
     * The old files should be deleted after merging.
     *
     * @param progressFlow The flow to emit the merge progress.
     * @param context The context of the downloading track.
     * @param files The files to merge.
     */
    suspend fun merge(
        progressFlow: MutableStateFlow<Progress>,
        context: DownloadContext,
        files: List<File>
    ): File

    /**
     * Tag a file with the given track metadata
     * use this to tag the file with the track metadata
     * ```
     * val track = context.track
     * ```
     *
     * @param context The extension that the track belongs to.
     * @param file The file to tag
     */
    suspend fun tag(
        progressFlow: MutableStateFlow<Progress>,
        context: DownloadContext,
        file: File
    ): File
}