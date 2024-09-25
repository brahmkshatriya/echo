package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.TrackerExtension

interface TrackerClientsProvider {
    val requiredTrackerClients: List<String>
    fun setTrackerExtensions(trackerClients: List<TrackerExtension>)
}