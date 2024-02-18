package tel.jeelpa.plugger.pluginloader.file

import android.content.Context
import kotlinx.serialization.Serializable
import tel.jeelpa.plugger.ManifestParser
import tel.jeelpa.plugger.getClassLoader
import tel.jeelpa.plugger.models.PluginMetadata
import tel.jeelpa.plugger.parsed

class FilePluginManifestParser(
    private val context: Context
) : ManifestParser<String> {

    @Serializable
    data class FilePluginManifest(
        val className: String,
    )

    override fun parseManifest(data: String): PluginMetadata {
        val manifestData = context.getClassLoader(data)
            .getResourceAsStream("manifest.json")
            .readBytes()
            .toString(Charsets.UTF_8)
            .parsed<FilePluginManifest>()

        return PluginMetadata(manifestData.className, data)
    }
}
