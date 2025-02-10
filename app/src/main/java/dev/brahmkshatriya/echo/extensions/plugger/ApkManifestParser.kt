package dev.brahmkshatriya.echo.extensions.plugger

import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.ManifestParser

class ApkManifestParser(
    private val importType: ImportType
) : ManifestParser<AppInfo, Metadata> {
    override fun parseManifest(data: AppInfo): Metadata = with(data.appInfo.metaData) {
        fun getOrNull(key: String) = getString(key)?.takeIf { it.isNotBlank() }
        fun get(key: String) = getOrNull(key)
            ?: error("$key not found in Metadata for ${data.appInfo.packageName}")

        Metadata(
            path = data.path,
            preservedPackages = getOrNull("preserved_packages")
                .orEmpty().split(",").map { it.trim() },
            className = get("class"),
            importType = importType,
            id = get("id"),
            version = get("version"),
            iconUrl = getOrNull("icon_url"),
            name = get("name"),
            description = get("description"),
            author = get("author"),
            authorUrl = getOrNull("author_url"),
            repoUrl = getOrNull("repo_url"),
            updateUrl = getOrNull("update_url"),
            enabled = getBoolean("enabled", true)
        )
    }
}