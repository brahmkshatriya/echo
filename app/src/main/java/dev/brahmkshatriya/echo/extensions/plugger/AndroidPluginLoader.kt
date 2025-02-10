package dev.brahmkshatriya.echo.extensions.plugger

import android.content.Context
import android.os.Build
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.PluginLoader
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class AndroidPluginLoader<TPlugin>(
    private val context: Context
) : PluginLoader<Metadata, TPlugin> {

    private fun getClassLoader(
        preservedPackages: List<String>, path: String, libFolder: String
    ) = ClassLoaderWithPreserved(
        preservedPackages, path, null, libFolder, context.classLoader
    )

    @Suppress("UNCHECKED_CAST")
    override fun loadPlugin(pluginMetadata: Metadata): TPlugin {
        val libFolder = unloadLibraries(pluginMetadata)
        return getClassLoader(
            pluginMetadata.preservedPackages,
            pluginMetadata.path,
            libFolder.absolutePath
        ).loadClass(pluginMetadata.className).getConstructor().newInstance() as TPlugin
    }

    private fun unloadLibraries(
        metadata: Metadata
    ): File {
        val targetAbi = Build.SUPPORTED_ABIS.first()
        val libFolder = File(context.cacheDir, "libs")
        if (!libFolder.exists()) libFolder.mkdirs()
        val libs = File(libFolder, metadata.id)
        if (!libs.exists()) extractLibsFromApk(metadata.path, targetAbi, libs).getOrThrow()
        return libs
    }

    private fun extractLibsFromApk(
        apkPath: String, targetAbi: String, outputFolder: File
    ) = runCatching {
        outputFolder.mkdirs()
        val apkFile = ZipFile(apkPath)
        val extractedFiles = mutableListOf<File>()
        apkFile.entries().iterator().forEach { entry ->
            if (entry.name.startsWith("lib/$targetAbi/") && entry.name.endsWith(".so")) {
                val fileName = entry.name.substringAfterLast("/")
                val outputFile = File(outputFolder, fileName)
                apkFile.getInputStream(entry).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                extractedFiles.add(outputFile)
            }
        }
        apkFile.close()
        extractedFiles
    }
}