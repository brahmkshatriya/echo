package dev.brahmkshatriya.echo.common.clients

interface ArtistFollowClient {
    fun followArtist(artist: ArtistClient)
    fun unfollowArtist(artist: ArtistClient)
}