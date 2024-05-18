package dev.brahmkshatriya.echo.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.dao.DownloadDao
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val download = runBlocking {
                withContext(Dispatchers.IO) { downloadDao.getDownload(downloadId) }
            } ?: return
            val track =
                context.getFromCache(download.itemId, Track.creator, "downloads") ?: return

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val fileUri = cursor.getString(columnIndex)
                if (fileUri != null) {
                    val mediaTypeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                    val mimeType = cursor.getString(mediaTypeIndex)
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    val file = File(Uri.parse(fileUri).path!!)
                    val newFile = File("${file.absolutePath}.$extension")
                    file.renameTo(newFile)
                    context.applyTags(newFile, track)
                    MediaScannerConnection.scanFile(
                        context, arrayOf(newFile.toString()), null, null
                    )
                }
            }
            cursor.close()
            runBlocking {
                withContext(Dispatchers.IO) { downloadDao.deleteDownload(downloadId) }
            }
        }
    }

    companion object {
        fun Context.applyTags(
            newFile: File, track
            : Track
        ) {
            val fd = ParcelFileDescriptor.open(
                newFile, ParcelFileDescriptor.MODE_READ_WRITE
            )

            val metadata = TagLib.getMetadata(fd = fd.dup().detachFd(), readPictures = true)
                ?: Metadata(PropertyMap(), arrayOf())
            val artwork = runBlocking { track.cover.loadBitmap(this@applyTags) }?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                Picture(stream.toByteArray(), "Back Cover", "Back Cover", "image/png")
            }

            artwork?.let { TagLib.savePictures(fd.dup().detachFd(), arrayOf(it)) }
            val props = metadata.propertyMap.apply {
                set("TITLE", arrayOf(track.title))
                set("ARTIST", arrayOf(track.artists.joinToString(", ") { it.name }))
                track.album?.run {
                    set("ALBUM", arrayOf(title))
                    set("ALBUMARTIST", arrayOf(artists.joinToString(", ") { it.name }))
                    releaseDate?.let { set("DATE", arrayOf(it)) }
                }
            }
            TagLib.savePropertyMap(fd.dup().detachFd(), props)
        }
    }
}