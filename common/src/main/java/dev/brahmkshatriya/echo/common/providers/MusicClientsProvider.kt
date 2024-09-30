package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.MusicExtension

interface MusicClientsProvider {
    val requiredMusicClients: List<String>
    fun setMusicExtensions(list: List<MusicExtension>)
}