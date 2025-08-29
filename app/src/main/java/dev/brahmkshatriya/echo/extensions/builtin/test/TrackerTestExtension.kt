package dev.brahmkshatriya.echo.extensions.builtin.test

import dev.brahmkshatriya.echo.common.clients.TrackerMarkClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

class TrackerTestExtension : TrackerMarkClient {
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

    override suspend fun getSettingItems() = listOf<Setting>()
    override fun setSettings(settings: Settings) {}

    override suspend fun onTrackChanged(details: TrackDetails?) {
        println("onTrackChanged ${details?.track?.id}")
    }

    override suspend fun getMarkAsPlayedDuration(details: TrackDetails): Long? {
        return details.totalDuration?.div(3)
    }

    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        println("onMarkAsPlayed: ${details.track.id}")
    }

    override suspend fun onPlayingStateChanged(details: TrackDetails?, isPlaying: Boolean) {
        println("onPlayingStateChanged $isPlaying: ${details?.track?.id}")
    }
}