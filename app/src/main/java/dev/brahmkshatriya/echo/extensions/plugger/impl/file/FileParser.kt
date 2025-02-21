package dev.brahmkshatriya.echo.extensions.plugger.impl.file

import android.content.pm.PackageManager
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.PACKAGE_FLAGS
import dev.brahmkshatriya.echo.extensions.plugger.impl.AppInfo
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.ManifestParser
import java.io.File

class FileParser(
    private val packageManager: PackageManager,
    private val apkManifestParser: ManifestParser<AppInfo, Metadata>
) : ManifestParser<File, Metadata> {
    override fun parseManifest(data: File): Metadata {
        val info = runCatching {
            AppInfo(
                data.path,
                packageManager.getPackageArchiveInfo(data.path, PACKAGE_FLAGS)!!
            )
        }.getOrElse {
            throw Exception("Failed to parse APK file: ${data.path}", it)
        }
        return apkManifestParser.parseManifest(info)
    }
}