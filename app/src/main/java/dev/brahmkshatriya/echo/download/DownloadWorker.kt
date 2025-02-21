package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.await
import kotlin.time.measureTime

class DownloadWorker(
    private val extensionLoader: ExtensionLoader,
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        println("Downloading... $extensionLoader")
        val list: List<Extension<*>>
        val time = measureTime {
            list = extensionLoader.extensions.all.await()
        }.inWholeMilliseconds
        println("$time ms: $list")
        println("Download complete")
        return Result.success()
    }
}