package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.FileTask
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
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
     * Get the download folder for the given [context]'s track.
     *
     * @param context The context of the downloading track.
     *
     * @return The download folder.
     *
     * @see DownloadContext
     * @see Track
     * @see EchoMediaItem
     */
    suspend fun getDownloadDir(context: DownloadContext): File

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
     * @param context The context of the downloading track.
     * @param source The source to download.
     * @param file The file to download the source to.
     *
     * @return The [FileTask] object.
     *
     * @see FileTask
     * @see Streamable.Source
     */
    suspend fun download(context: DownloadContext, source: Streamable.Source, file: File): FileTask

    /**
     * Merge the given media [files] into a single file.
     * The old files should be deleted after merging.
     *
     * @param context The context of the downloading track.
     * @param files The files to merge.
     *
     * @return The [FileTask] object.
     *
     * @see FileTask
     */
    suspend fun merge(context: DownloadContext, files: List<File>, dir: File): FileTask

    /**
     * Tag a file with the given track metadata
     * use this to tag the file with the track metadata
     * ```
     * val track = context.track
     * ```
     *
     * @param context The extension that the track belongs to.
     * @param file The file to tag
     * @return The task that will tag the file
     *
     * @see FileTask
     */
    suspend fun tag(context: DownloadContext, file: File): FileTask
}