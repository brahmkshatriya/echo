package dev.brahmkshatriya.echo.plugger.parser

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import tel.jeelpa.plugger.ManifestParser
import tel.jeelpa.plugger.pluginloader.apk.ApkPluginSource
import java.io.File

class ApkFileManifestParser(
    private val packageManager: PackageManager,
    private val apkManifestParser: ManifestParser<ApplicationInfo, ExtensionMetadata>
) : ManifestParser<File, ExtensionMetadata> {
    override fun parseManifest(data: File): ExtensionMetadata {
        return apkManifestParser.parseManifest(
            packageManager
                .getPackageArchiveInfo(data.path, ApkPluginSource.PACKAGE_FLAGS)!!
                .applicationInfo!!
        )
    }
}