package dev.brahmkshatriya.echo.extensions.plugger

import android.content.pm.PackageManager
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.ManifestParser
import java.io.File

class ApkFileManifestParser(
    private val packageManager: PackageManager,
    private val apkManifestParser: ManifestParser<AppInfo, Metadata>
) : ManifestParser<File, Metadata> {
    override fun parseManifest(data: File): Metadata {
        val info = runCatching {
            packageManager
                .getPackageArchiveInfo(data.path, ApkPluginSource.PACKAGE_FLAGS)!!
                .applicationInfo!!
        }.getOrElse {
            throw Exception("Failed to parse APK file: ${data.path}")
        }
        return apkManifestParser.parseManifest(AppInfo(data.path, info))
    }
}