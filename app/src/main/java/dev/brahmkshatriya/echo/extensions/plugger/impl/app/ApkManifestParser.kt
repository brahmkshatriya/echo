package dev.brahmkshatriya.echo.extensions.plugger.impl.app

import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.impl.AppInfo
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.ManifestParser

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
            type = data.type,
            id = get("id"),
            version = get("version"),
            icon = getOrNull("icon_url")?.toImageHolder(),
            name = get("name"),
            description = get("description"),
            author = get("author"),
            authorUrl = getOrNull("author_url"),
            repoUrl = getOrNull("repo_url"),
            updateUrl = getOrNull("update_url"),
            isEnabled = getBoolean("enabled", true)
        )
    }
}