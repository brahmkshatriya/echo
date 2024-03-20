package dev.brahmkshatriya.echo.data.extensions

import android.content.pm.ApplicationInfo
import tel.jeelpa.plugger.ManifestParser
import tel.jeelpa.plugger.models.PluginMetadata

class ApkManifestParser: ManifestParser<ApplicationInfo> {
    override fun parseManifest(data: ApplicationInfo): PluginMetadata {

        return PluginMetadata(
            path = data.sourceDir,
            className = data.metaData.getString("class")
                ?: error("Class Name not found in Metadata for ${data.packageName}"),
        )
    }
}