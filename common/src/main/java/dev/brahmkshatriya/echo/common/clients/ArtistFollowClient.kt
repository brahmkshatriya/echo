package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Artist

/**
 * Used to allow following and unfollowing an [Artist].
 *
 * @see ArtistClient
 * @see MusicExtension
 */
interface ArtistFollowClient : ArtistClient {

    /**
     * Follows/Unfollow an artist.
     * The artist will be loaded again after following/unfollowing.
     *
     * @param artist the artist to follow.
     * @param follow whether to follow or unfollow the artist.
     *
     * @see Artist.isFollowing
     */
    suspend fun followArtist(artist: Artist, follow: Boolean)
}