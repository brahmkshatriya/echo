package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.LyricsExtension

interface LyricsClientsProvider {
    val requiredLyricsClients: List<String>
    fun setLyricsExtensions(list: List<LyricsExtension>)
}