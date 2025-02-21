package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.brahmkshatriya.echo.extensions.ExtensionLoader

class Downloader(
    context: Context,
    private val extensionLoader: ExtensionLoader
) {

    private val workManager = WorkManager.getInstance(context)
    fun start() {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints(NetworkType.CONNECTED, requiresStorageNotLow = true))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(TAG, androidx.work.ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val TAG = "DOWNLOAD_WORKER"
    }
}