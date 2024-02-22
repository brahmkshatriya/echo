package dev.brahmkshatriya.echo.player

import dev.brahmkshatriya.echo.common.models.Track

object Global {
    val queue = mutableListOf<Pair<String, Track>>()
    fun getTrack(mediaId:String) = queue.find { it.first == mediaId }?.second
}