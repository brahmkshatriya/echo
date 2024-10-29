package dev.brahmkshatriya.echo.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import com.arthenica.ffmpegkit.FFmpegKit
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
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return
        if (action == "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE") {
            val downloadId = intent.getLongExtra("downloadId", -1)

            val download = runBlocking {
                withContext(Dispatchers.IO) { downloadDao.getDownload(downloadId) }
            } ?: return
            val track = context.applicationContext.getFromCache<Track>(download.itemId, "downloads") ?: return

            val file = File(download.downloadPath)

            if (file.exists()) {
                writeM4ATag(file, track)
                MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
                runBlocking { withContext(Dispatchers.IO) { downloadDao.deleteDownload(downloadId) } }
            }
        }
    }

    companion object {

        private val client = OkHttpClient.Builder().build()

        private fun saveCoverBitmap(file: File, track: Track): File? {
            return try {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        val holder = track.cover as? ImageHolder.UrlRequestImageHolder
                            ?: throw IllegalArgumentException("Invalid ImageHolder type")

                        val request = Request.Builder()
                            .url(holder.request.url)
                            .build()

                        client.newCall(request).execute().use { response ->

                            val responseBody = response.body
                            val bytes = responseBody.bytes()

                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            val coverFile = File(file.parent, "cover_temp.jpeg")

                            FileOutputStream(coverFile).use { fos ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            }

                            coverFile
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error saving cover bitmap: $e")
                null
            }
        }


        fun writeM4ATag(file: File, track: Track) {
            try {
                val coverFile = saveCoverBitmap(file, track)

                val outputFile = File(file.parent, "temp_${file.name}")

                val metadataTitle = "title=\"${track.title}\""
                val metadataArtist = "artist=\"${track.artists.joinToString(", ") { it.name }}\""
                val metadataAlbum = "album=\"${track.album?.title ?: ""}\""

                val metadataCoverTitle = "title=\"Album cover\""
                val metadataCoverComment = "comment=\"Cover (front)\""

                val cmd = when (val fileExtension = file.extension.lowercase()) {
                    "m4a", "flac" -> {
                        arrayOf(
                            "-i", "\"${file.absolutePath}\"",
                            "-i", "\"${coverFile?.absolutePath}\"",
                            "-c", "copy",
                            "-c:v", "mjpeg",
                            "-metadata", metadataTitle,
                            "-metadata", metadataArtist,
                            "-metadata", metadataAlbum,
                            "-metadata:s:v", metadataCoverTitle,
                            "-metadata:s:v", metadataCoverComment,
                            "-disposition:v", "attached_pic",
                            "\"${outputFile.absolutePath}\""
                        )
                    }
                    "mp3" -> {
                        arrayOf(
                            "-i", "\"${file.absolutePath}\"",
                            "-i", "\"${coverFile?.absolutePath}\"",
                            "-map", "0:0",
                            "-map", "1:0",
                            "-c", "copy",
                            "-id3v2_version", "4",
                            "-metadata", metadataTitle,
                            "-metadata", metadataArtist,
                            "-metadata", metadataAlbum,
                            "-metadata:s:v", metadataCoverTitle,
                            "-metadata:s:v", metadataCoverComment,
                            "\"${outputFile.absolutePath}\""
                        )
                    }
                    else -> throw IllegalArgumentException("Unsupported file format: .$fileExtension")
                }

                val rc = FFmpegKit.execute(cmd.joinToString(" ")).returnCode.value

                if (rc == 0) {
                    if (file.delete()) {
                        outputFile.renameTo(file)

                    }
                }

                coverFile?.delete()

            } catch (e: Exception) {
                println("Error writing M4A tags with artwork: ${e.message}")
            }
        }
    }
}