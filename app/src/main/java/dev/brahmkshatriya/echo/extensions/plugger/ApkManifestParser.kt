package dev.brahmkshatriya.echo.extensions.plugger

import android.content.pm.ApplicationInfo
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.ManifestParser

class ApkManifestParser(
    private val importType: ImportType
) : ManifestParser<ApplicationInfo, Metadata> {
    override fun parseManifest(data: ApplicationInfo) = with(data.metaData) {
        fun get(key: String): String = getString(key)
            ?: error("$key not found in Metadata for ${data.packageName}")

        Metadata(
            path = data.sourceDir,
            className = get("class"),
            importType = importType,
            id = get("id") + importType.name,
            name = get("name"),
            version = get("version"),
            description = get("description"),
            author = get("author"),
            iconUrl = getString("icon_url"),
            enabled = getBoolean("enabled", true)
        )
    }
}