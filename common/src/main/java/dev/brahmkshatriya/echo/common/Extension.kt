package dev.brahmkshatriya.echo.common

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditCoverClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditPrivacyClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackHideClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.LyricsExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MessageFlowProvider
import dev.brahmkshatriya.echo.common.providers.MetadataProvider
import dev.brahmkshatriya.echo.common.providers.MiscExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerExtensionsProvider

/**
 * A base class for any of the following Extension types
 * - [MusicExtension] - To play music and load music data (Supports tracking and lyrics too)
 * - [TrackerExtension] - To track what the user is listening to
 * - [LyricsExtension] - To show lyrics for the currently playing track
 *
 * @param T The type of the extension client
 * @property metadata The metadata of the extension
 * @property instance An injectable instance of the [T] client
 */
sealed class Extension<T : ExtensionClient>(
    open val metadata: Metadata,
    open val instance: Injectable<T>
) {
    /**
     * The id of the extension
     */
    val id: String get() = metadata.id

    /**
     * The type of the extension
     */
    val type: ExtensionType get() = metadata.type

    /**
     * Whether the extension is enabled
     */
    val isEnabled: Boolean get() = metadata.isEnabled

    /**
     * The name of the extension
     */
    val name: String get() = metadata.name

    /**
     * The version of the extension
     */
    val version: String get() = metadata.version
}

/**
 * A data class representing a Music Extension.
 * If some function is not supported by the extension, it should throw a [ClientException].
 *
 * Music Extension supports the following types of clients:
 * - [ExtensionClient] - Mandatory Base Client
 * - [LoginClient] - For login support
 * - [ShareClient] - For showing a share button on media items
 *
 * ### Feed
 * To show feed of media items on main screen, the extension should implement the following clients:
 * - [HomeFeedClient] - To load the feed on the Home Tab
 * - [SearchFeedClient] - To load the feed on the Search Tab
 * - [LibraryFeedClient] - To load the feed on the Library Tab
 *
 * ### Media Items
 * To load media items, the extension should implement the following clients:
 * - [AlbumClient] - To load albums
 * - [PlaylistClient] - To load playlists
 * - [ArtistClient] - To load artists
 * - [UserClient] - To load user data
 *
 * ### Track Streaming
 * When streaming a track, the extension can implement the following clients:
 * - [TrackClient] - Mandatory to stream tracks
 * - [RadioClient] - To load next tracks for the current track & show radio button on media items
 * - [TrackerClient] - For tracking what the user is listening to
 * - [LyricsClient] - For lyrics support
 * - [LyricsSearchClient] - For searching lyrics using user query
 *
 * ### Library
 * To allow library functionality, The extension can implement the following clients:
 * - [ArtistFollowClient] - To follow/unfollow artists
 * - [TrackLikeClient] - To like/unlike tracks
 * - [SaveToLibraryClient] - To save media items to the library
 * - [TrackHideClient] - To hide tracks
 *
 * ### Playlist Editing
 * To allow playlist editing, The extension can implement the following clients:
 * - [PlaylistEditClient] - To edit playlists and show create playlist button on Library Tab
 * - [PlaylistEditCoverClient] - To edit playlist cover
 * - [PlaylistEditorListenerClient] - To listen to playlist editing events
 * - [PlaylistEditPrivacyClient] - To edit playlist privacy
 *
 * ## Providers
 * The extension can also implement the following providers:
 * - [MetadataProvider] - To get metadata of the extension
 * - [MessageFlowProvider] - To send messages in the app
 * - [MusicExtensionsProvider] - To get installed music extensions
 * - [LyricsExtensionsProvider] - To get installed lyrics extensions
 * - [TrackerExtensionsProvider] - To get installed tracker extensions
 * - [MiscExtensionsProvider] - To get installed misc extensions
 *
 * @param metadata The metadata of the extension
 * @param instance An injectable instance of the [ExtensionClient] client
 */
data class MusicExtension(
    override val metadata: Metadata,
    override val instance: Injectable<ExtensionClient>
) : Extension<ExtensionClient>(metadata, instance)

/**
 * A data class representing a Tracker Extension.
 *
 * Tracker Extension supports the following types of clients:
 * - [TrackerClient] - Mandatory, For tracking what the user is listening to
 * - [LoginClient] - For login support
 *
 * The extension can also implement the following providers:
 * - [MetadataProvider] - To get metadata of the extension
 * - [MessageFlowProvider] - To send messages in the app
 * - [MusicExtensionsProvider] - To get installed music extensions
 * - [LyricsExtensionsProvider] - To get installed lyrics extensions
 * - [TrackerExtensionsProvider] - To get installed tracker extensions
 * - [MiscExtensionsProvider] - To get installed misc extensions
 *
 * @param metadata The metadata of the extension
 * @param instance An injectable instance of the [TrackerClient] client
 */
data class TrackerExtension(
    override val metadata: Metadata,
    override val instance: Injectable<TrackerClient>
) : Extension<TrackerClient>(metadata, instance)

/**
 * A data class representing a Lyrics Extension.
 * Lyrics Extension supports the following types of clients:
 * - [LyricsClient] - Mandatory, For lyrics support
 * - [LyricsSearchClient] - For searching lyrics using user query
 * - [LoginClient] - For login support
 *
 * The extension can also implement the following providers:
 * - [MetadataProvider] - To get metadata of the extension
 * - [MessageFlowProvider] - To send messages in the app
 * - [MusicExtensionsProvider] - To get installed music extensions
 * - [LyricsExtensionsProvider] - To get installed lyrics extensions
 * - [TrackerExtensionsProvider] - To get installed tracker extensions
 * - [MiscExtensionsProvider] - To get installed misc extensions
 *
 * @param metadata The metadata of the extension
 * @param instance An injectable instance of the [LyricsClient] client
 */
data class LyricsExtension(
    override val metadata: Metadata,
    override val instance: Injectable<LyricsClient>
) : Extension<LyricsClient>(metadata, instance)


/**
 * A data class representing a Misc Extension.
 * Misc Extension supports the following types of clients:
 * - [ExtensionClient] - Mandatory Base Client
 * - [DownloadClient] - For downloading tracks
 * - [LoginClient] - For login support
 *
 * The extension can also implement the following providers:
 * - [MetadataProvider] - To get metadata of the extension
 * - [MessageFlowProvider] - To send messages in the app
 * - [MusicExtensionsProvider] - To get installed music extensions
 * - [LyricsExtensionsProvider] - To get installed lyrics extensions
 * - [TrackerExtensionsProvider] - To get installed tracker extensions
 * - [MiscExtensionsProvider] - To get installed misc extensions
 *
 * @param metadata The metadata of the extension
 * @param instance An injectable instance of the [ExtensionClient] client
 */
data class MiscExtension(
    override val metadata: Metadata,
    override val instance: Injectable<ExtensionClient>
) : Extension<ExtensionClient>(metadata, instance)