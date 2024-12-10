package dev.brahmkshatriya.echo.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.utils.getFromCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
                context.getFromCache<Track>(download.itemId, "downloads") ?: return

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
}