package dev.brahmkshatriya.echo.extensions.builtin.test

import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

class TrackerTestExtension : TrackerClient {

    companion object {
        val metadata = Metadata(
            "TrackerTestExtension",
            "",
            ImportType.BuiltIn,
            ExtensionType.TRACKER,
            "test",
            "Tracker Test Extension",
            "1.0.0",
            "Test extension for offline testing",
            "Test",
        )
    }

    override val settingItems = listOf<Setting>()
    override fun setSettings(settings: Settings) {}

    override suspend fun onTrackChanged(details: TrackDetails?) {
        println("onTrackChanged ${details?.track?.id}")
    }

    override val markAsPlayedDuration = 30000L

    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        println("onMarkAsPlayed: ${details.track.id}")
    }

    override suspend fun onPlayingStateChanged(details: TrackDetails?, isPlaying: Boolean) {
        println("onPlayingStateChanged $isPlaying: ${details?.track?.id}")
    }
}