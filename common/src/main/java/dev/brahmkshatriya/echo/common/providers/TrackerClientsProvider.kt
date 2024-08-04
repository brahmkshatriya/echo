package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.clients.TrackerClient

interface TrackerClientsProvider {
    val requiredTrackerClients: List<String>
    fun setTrackerClients(trackerClients: List<Pair<String, TrackerClient>>)
}