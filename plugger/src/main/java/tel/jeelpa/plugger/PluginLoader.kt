package tel.jeelpa.plugger

import tel.jeelpa.plugger.models.PluginMetadata

interface PluginLoader {
    operator fun <TPlugin> invoke(pluginMetadata: PluginMetadata): TPlugin
}