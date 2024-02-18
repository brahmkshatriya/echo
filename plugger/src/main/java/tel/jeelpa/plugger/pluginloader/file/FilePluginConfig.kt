package tel.jeelpa.plugger.pluginloader.file


data class FilePluginConfig(
    val path: String,
    val extension: String,
    val childFolder: String? = null
)
