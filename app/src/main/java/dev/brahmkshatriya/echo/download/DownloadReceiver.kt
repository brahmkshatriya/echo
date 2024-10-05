package dev.brahmkshatriya.echo.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: EchoDatabase

    private val downloadDao: DownloadDao by lazy { database.downloadDao() }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return
        if ("dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE" == action) {
            val downloadId = intent.getLongExtra("downloadId", -1)
            val download = runBlocking {
                withContext(Dispatchers.IO) { downloadDao.getDownload(downloadId) }
            } ?: return
            val track = context.getFromCache<Track>(download.itemId, "downloads") ?: return

            Log.i("FUCK YOU", download.downloadPath)

            val file = File(download.downloadPath)

            if (file.exists()) {
                context.writeID3v2Tag(file, track)
                MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
                runBlocking { withContext(Dispatchers.IO) { downloadDao.deleteDownload(downloadId) } }
            }
        }
    }

    companion object {
        fun Context.writeID3v2Tag(file: File, track: Track) {
            try {
                // Set to overwrite existing tags
                TagOptionSingleton.getInstance().isId3v2Save = false
                val audioFile: AudioFile = AudioFileIO.readAs(file, "m4a")
                val tag: Tag = audioFile.tagAndConvertOrCreateAndSetDefault

                // Set the tags
                tag.addField(FieldKey.TITLE, track.title)
                tag.addField(FieldKey.ARTIST, track.artists.joinToString(", ") { it.name })
                tag.addField(FieldKey.ALBUM, track.album?.title ?: "")
                tag.setField(FieldKey.YEAR, track.album?.releaseDate?.substring(0, 4) ?: "")

                // Set the album cover
                val coverBitmap = runBlocking { track.cover.loadBitmap(this@writeID3v2Tag) }
                coverBitmap?.let {
                    val stream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val artwork = ArtworkFactory.createLinkedArtworkFromURL(track.cover.toString())
                    artwork.binaryData = stream.toByteArray()
                    artwork.mimeType = "image/png"
                    tag.deleteArtworkField() // Remove existing cover art if any
                    tag.addField(artwork)
                }

                // Save the changes
                AudioFileIO.write(audioFile)

            } catch (e: CannotWriteException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}