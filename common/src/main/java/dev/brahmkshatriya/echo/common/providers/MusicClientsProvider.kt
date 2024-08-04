package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.clients.ExtensionClient

interface MusicClientsProvider {
    val requiredMusicClients: List<String>
    fun setMusicClients(list: List<Pair<String, ExtensionClient>>)
}