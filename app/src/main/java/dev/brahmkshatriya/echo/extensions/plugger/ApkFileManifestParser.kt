package dev.brahmkshatriya.echo.extensions.plugger

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.ManifestParser
import java.io.File

class ApkFileManifestParser(
    private val packageManager: PackageManager,
    private val apkManifestParser: ManifestParser<ApplicationInfo, Metadata>
) : ManifestParser<File, Metadata> {
    override fun parseManifest(data: File): Metadata {
        return apkManifestParser.parseManifest(
            packageManager
                .getPackageArchiveInfo(data.path, ApkPluginSource.PACKAGE_FLAGS)!!
                .applicationInfo!!
        )
    }
}