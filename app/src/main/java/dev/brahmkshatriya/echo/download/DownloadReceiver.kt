package dev.brahmkshatriya.echo.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.utils.getFromCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: EchoDatabase

    private val downloadDao: DownloadDao by lazy { database.downloadDao() }

    private val client = OkHttpClient.Builder().build()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return
        if (action == "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE") {
            val downloadId = intent.getLongExtra("downloadId", -1)
            val order = intent.getIntExtra("order", 0)
            if (downloadId == -1L) return

            val pendingResult = goAsync()
            coroutineScope.launch {
                try {
                    handleDownloadComplete(context, downloadId, order)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun handleDownloadComplete(context: Context, downloadId: Long, order: Int) {
        val download = withContext(Dispatchers.IO) { downloadDao.getDownload(downloadId) }
        if (download == null) {
            Log.e("DownloadReceiver", "Download record not found for ID: $downloadId")
            return
        }

        val track = withContext(Dispatchers.IO) {
            context.applicationContext.getFromCache<Track>(download.itemId, "downloads")
        } ?: run {
            Log.e("DownloadReceiver", "Track not found in cache for ID: ${download.itemId}")
            return
        }

        val file = File(download.downloadPath)
        if (file.exists()) {
            writeM4ATag(file, track, order)

            withContext(Dispatchers.Main) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    null,
                    null
                )
            }

            withContext(Dispatchers.IO) {
                downloadDao.deleteDownload(downloadId)
            }
        } else {
            Log.e("DownloadReceiver", "Downloaded file does not exist: ${file.absolutePath}")
        }
    }

    private suspend fun saveCoverBitmap(file: File, track: Track): File? = withContext(Dispatchers.IO) {
        try {
            val holder = track.cover as? ImageHolder.UrlRequestImageHolder
                ?: throw IllegalArgumentException("Invalid ImageHolder type")

            val request = Request.Builder()
                .url(holder.request.url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download cover image")

                val bytes = response.body.bytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw Exception("Failed to decode bitmap")

                val coverFile = File(file.parent, "cover_temp.jpeg")
                FileOutputStream(coverFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                coverFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val illegalChars = "[/\\\\:*?\"<>|]".toRegex()

    private suspend fun writeM4ATag(file: File, track: Track, order: Int) = withContext(Dispatchers.IO) {
        try {
            val coverFile = runBlocking { saveCoverBitmap(file, track) }

            val outputFile = File(file.parent, "temp_${file.name}")

            val metadataOrder = "track=\"$order\""
            val metadataTitle = "title=\"${illegalChars.replace(track.title, "_")}\""
            val metadataArtist = "artist=\"${track.artists.joinToString(", ") { it.name }}\""
            val metadataAlbum = "album=\"${illegalChars.replace(track.album?.title.orEmpty(), "_")}\""

            val metadataCoverTitle = "title=\"Album cover\""
            val metadataCoverComment = "comment=\"Cover (front)\""

            val cmd = when (file.extension.lowercase()) {
                "m4a", "flac" ->
                    arrayOf(
                        "-i", "\"${file.absolutePath}\"",
                        "-i", "\"${coverFile?.absolutePath}\"",
                        "-c", "copy",
                        "-c:v", "mjpeg",
                        "-metadata", metadataOrder,
                        "-metadata", metadataTitle,
                        "-metadata", metadataArtist,
                        "-metadata", metadataAlbum,
                        "-metadata:s:v", metadataCoverTitle,
                        "-metadata:s:v", metadataCoverComment,
                        "-disposition:v", "attached_pic",
                        "\"${outputFile.absolutePath}\""
                    )

                "mp3" ->
                    arrayOf(
                        "-i", "\"${file.absolutePath}\"",
                        "-i", "\"${coverFile?.absolutePath}\"",
                        "-map", "0:0",
                        "-map", "1:0",
                        "-c", "copy",
                        "-id3v2_version", "4",
                        "-metadata", metadataOrder,
                        "-metadata", metadataTitle,
                        "-metadata", metadataArtist,
                        "-metadata", metadataAlbum,
                        "-metadata:s:v", metadataCoverTitle,
                        "-metadata:s:v", metadataCoverComment,
                        "\"${outputFile.absolutePath}\""
                    )

                else -> throw IllegalArgumentException("Unsupported file format: .${file.extension}")
            }

            val rc = FFmpegKit.execute(cmd.joinToString(" ")).returnCode


            if (ReturnCode.isSuccess(rc)) {
                if (file.delete()) {
                    outputFile.renameTo(file)
                }
            }

            coverFile?.delete()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}