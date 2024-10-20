package dev.brahmkshatriya.echo.extensions.plugger

import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.ManifestParser

class ApkManifestParser(
    private val importType: ImportType
) : ManifestParser<AppInfo, Metadata> {
    override fun parseManifest(data: AppInfo): Metadata = with(data.appInfo.metaData) {
        fun get(key: String): String = getString(key)
            ?: error("$key not found in Metadata for ${data.appInfo.packageName}")

        Metadata(
            path = data.path,
            className = get("class"),
            importType = importType,
            id = get("id") + importType.name,
            name = get("name"),
            version = get("version"),
            description = get("description"),
            author = get("author"),
            authorUrl = getString("author_url"),
            iconUrl = getString("icon_url"),
            repoUrl = getString("repo_url"),
            updateUrl = getString("update_url"),
            enabled = getBoolean("enabled", true)
        )
    }
}