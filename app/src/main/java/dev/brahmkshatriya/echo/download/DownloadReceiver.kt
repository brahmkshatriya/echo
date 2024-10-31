package dev.brahmkshatriya.echo.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.utils.getFromCache
import kotlinx.coroutines.Dispatchers
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

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return
        if (action == "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE") {
            val downloadId = intent.getLongExtra("downloadId", -1)
            if (downloadId == -1L) return

            handleDownloadComplete(context, downloadId)

        }
    }

    private fun handleDownloadComplete(context: Context, downloadId: Long) {
        val download = runBlocking { withContext(Dispatchers.IO) { downloadDao.getDownload(downloadId) } }
        val track = context.applicationContext.getFromCache<Track>(download?.itemId.orEmpty(), "downloads") ?: return

        val file = File(download?.downloadPath.orEmpty())
        if (file.exists()) {
            writeM4ATag(file, track)
            MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
            runBlocking { withContext(Dispatchers.IO) { downloadDao.deleteDownload(downloadId) } }
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

    private fun writeM4ATag(file: File, track: Track) {
        try {
            val coverFile = runBlocking { saveCoverBitmap(file, track) }

            val outputFile = File(file.parent, "temp_${file.name}")

            val metadata = listOf(
                "title=\"${track.title}\"",
                "artist=\"${track.artists.joinToString(", ") { it.name }}\"",
                "album=\"${track.album?.title ?: ""}\"",
                "title=\"Album cover\"",
                "comment=\"Cover (front)\""
            )

            val cmd = when (file.extension.lowercase()) {
                "m4a", "flac" -> listOf(
                    "-i", "\"${file.absolutePath}\"",
                    "-i", "\"${coverFile?.absolutePath}\"",
                    "-c", "copy",
                    "-c:v", "mjpeg",
                    "-metadata", metadata[0],
                    "-metadata", metadata[1],
                    "-metadata", metadata[2],
                    "-metadata:s:v", metadata[3],
                    "-metadata:s:v", metadata[4],
                    "-disposition:v", "attached_pic",
                    "\"${outputFile.absolutePath}\""
                )
                "mp3" -> listOf(
                    "-i", "\"${file.absolutePath}\"",
                    "-i", "\"${coverFile?.absolutePath}\"",
                    "-map", "0:0",
                    "-map", "1:0",
                    "-c", "copy",
                    "-id3v2_version", "4",
                    "-metadata", metadata[0],
                    "-metadata", metadata[1],
                    "-metadata", metadata[2],
                    "-metadata:s:v", metadata[3],
                    "-metadata:s:v", metadata[4],
                    "\"${outputFile.absolutePath}\""
                )
                else -> throw IllegalArgumentException("Unsupported file format: .${file.extension}")
            }

            val ffmpegCommand = cmd.joinToString(" ")

            val session = FFmpegKit.execute(ffmpegCommand)
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
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