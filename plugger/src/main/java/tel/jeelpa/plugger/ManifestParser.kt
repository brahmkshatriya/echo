package tel.jeelpa.plugger

import tel.jeelpa.plugger.models.PluginMetadata


interface ManifestParser<T> {
    fun parseManifest(data: T): PluginMetadata
}

