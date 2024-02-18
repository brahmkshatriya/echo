package tel.jeelpa.plugger.models

data class PluginConfiguration(
    val packagePrefix: String,
    val featureName: String = "$packagePrefix.extension",
    val metadataSourceClassTag: String = "$packagePrefix.sourceclass",
)
