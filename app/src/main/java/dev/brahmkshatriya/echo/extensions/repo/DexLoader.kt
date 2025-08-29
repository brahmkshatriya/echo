package dev.brahmkshatriya.echo.extensions.repo

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import dev.brahmkshatriya.echo.common.models.Metadata
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class DexLoader(
    metadata: Metadata,
    context: Context,
    librarySearchPath: String =
        unloadLibraries(metadata, File(context.cacheDir, "libs")).absolutePath
) : DexClassLoader(
    metadata.path, context.cacheDir.absolutePath, librarySearchPath, context.classLoader
) {

    private val preservedPackages = metadata.preservedPackages
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

        private fun unloadLibraries(
            metadata: Metadata, libFolder: File
        ): File {
            val targetAbi = Build.SUPPORTED_ABIS.first()
            if (!libFolder.exists()) libFolder.mkdirs()
            val libs = File(libFolder, metadata.id)
            val version = File(libs, "version.txt")
            if (version.exists() && version.readText() == metadata.version) return libs
            libs.deleteRecursively()
            extractLibsFromApk(metadata.path, targetAbi, libs).getOrThrow()
            version.writeText(metadata.version)
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

}