package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.clients.LyricsClient

interface LyricsClientsProvider {
    val requiredLyricsClients: List<String>
    fun setLyricsClients(list: List<Pair<String, LyricsClient>>)
}