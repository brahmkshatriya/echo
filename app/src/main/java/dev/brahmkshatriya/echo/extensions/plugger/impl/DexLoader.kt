package dev.brahmkshatriya.echo.extensions.plugger.impl

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginLoader
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class DexLoader(
    private val context: Context
) : PluginLoader<Metadata, ExtensionClient> {

    private fun getClassLoader(
        preservedPackages: List<String>, path: String, libFolder: String
    ) = ClassLoaderWithPreserved(
        preservedPackages, path, context.cacheDir.absolutePath, libFolder, context.classLoader
    )

    override fun loadPlugin(pluginMetadata: Metadata): ExtensionClient {
        val libFolder = unloadLibraries(pluginMetadata)
        val clazz = getClassLoader(
            pluginMetadata.preservedPackages,
            pluginMetadata.path,
            libFolder.absolutePath
        ).loadClass(pluginMetadata.className)
        return clazz.getConstructor().newInstance() as ExtensionClient
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

    class ClassLoaderWithPreserved(
        private val preservedPackages: List<String>,
        dexPath: String,
        optimizedDirectory: String?,
        librarySearchPath: String,
        parent: ClassLoader
    ) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

        override fun loadClass(name: String?, resolve: Boolean): Class<*> {
            if (name != null && preservedPackages.any { name.startsWith(it) }) {
                val loadedClass = classMap[name]?.get()
                if (loadedClass != null) return loadedClass
                val clazz = super.loadClass(name, resolve)
                classMap[name] = WeakReference(clazz)
                return clazz
            }
            return super.loadClass(name, resolve)
        }

        companion object {
            private val classMap = mutableMapOf<String, WeakReference<Class<*>>>()
        }
    }
}