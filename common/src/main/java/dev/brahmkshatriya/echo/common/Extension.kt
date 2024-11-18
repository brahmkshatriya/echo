package dev.brahmkshatriya.echo.common

import dev.brahmkshatriya.echo.common.clients.ControllerClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Metadata

sealed class Extension<T : ExtensionClient>(
    val type: ExtensionType,
    open val metadata: Metadata,
    open val instance: Lazy<Result<T>>
){
    val id: String get() = metadata.id
    val name: String get() = metadata.name
    val version: String get() = metadata.version
}

data class MusicExtension(
    override val metadata: Metadata,
    override val instance: Lazy<Result<ExtensionClient>>,
) : Extension<ExtensionClient>(ExtensionType.MUSIC, metadata, instance)

data class TrackerExtension(
    override val metadata: Metadata,
    override val instance: Lazy<Result<TrackerClient>>,
) : Extension<TrackerClient>(ExtensionType.TRACKER, metadata, instance)

data class LyricsExtension(
    override val metadata: Metadata,
    override val instance: Lazy<Result<LyricsClient>>,
) : Extension<LyricsClient>(ExtensionType.LYRICS, metadata, instance)

data class ControllerExtension(
    override val metadata: Metadata,
    override val instance: Lazy<Result<ControllerClient>>,
) : Extension<ControllerClient>(ExtensionType.CONTROLLER, metadata, instance)