package dev.brahmkshatriya.echo.plugger.parser

import android.content.pm.ApplicationInfo
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import tel.jeelpa.plugger.ManifestParser

class ApkManifestParser : ManifestParser<ApplicationInfo, ExtensionMetadata> {
    override fun parseManifest(data: ApplicationInfo) = with(data.metaData) {
        fun get(key: String): String = getString(key)
            ?: error("$key not found in Metadata for ${data.packageName}")

        ExtensionMetadata(
            path = data.sourceDir,
            className = get("class"),
            id = get("id"),
            name = get("name"),
            version = get("version"),
            description = get("description"),
            author = get("author"),
            iconUrl = getString("icon_url")
        )
    }
}