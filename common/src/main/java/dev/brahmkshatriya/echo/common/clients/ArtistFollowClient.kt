package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Artist

interface ArtistFollowClient {
    suspend fun followArtist(artist: Artist) : Boolean
    suspend fun unfollowArtist(artist: Artist) : Boolean
}