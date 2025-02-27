package dev.brahmkshatriya.echo.utils

import android.content.Context
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import java.io.File

object CacheUtils {

    fun cacheDir(context: Context, folderName: String) =
        File(context.cacheDir, folderName).apply { mkdirs() }

    const val CACHE_FOLDER_SIZE = 50 * 1024 * 1024 //50MB

    inline fun <reified T> Context.saveToCache(
        id: String, data: T?, folderName: String = T::class.java.simpleName
    ) = runCatching {
        val fileName = id.hashCode().toString()
        val cacheDir = cacheDir(this, folderName)
        val file = File(cacheDir, fileName)

        var size = cacheDir.walk().sumOf { it.length().toInt() }
        while (size > CACHE_FOLDER_SIZE) {
            val files = cacheDir.listFiles()
            files?.sortBy { it.lastModified() }
            files?.firstOrNull()?.delete()
            size = cacheDir.walk().sumOf { it.length().toInt() }
        }
        file.writeText(data.toJson())
    }

    inline fun <reified T> Context.getFromCache(
        id: String, folderName: String = T::class.java.simpleName
    ): T? {
        val fileName = id.hashCode().toString()
        val cacheDir = cacheDir(this, folderName)
        val file = File(cacheDir, fileName)
        return if (file.exists()) runCatching {
            file.readText().toData<T>()
        }.getOrNull() else null
    }
}
