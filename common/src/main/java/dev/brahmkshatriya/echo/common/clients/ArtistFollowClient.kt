package dev.brahmkshatriya.echo.common.clients

interface ArtistFollowClient {
    suspend fun followArtist(artist: ArtistClient) : Boolean
    suspend fun unfollowArtist(artist: ArtistClient) : Boolean
}