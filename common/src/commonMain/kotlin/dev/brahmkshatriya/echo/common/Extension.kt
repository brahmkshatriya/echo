package dev.brahmkshatriya.echo.common

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditCoverClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditPrivacyClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.clients.QuickSearchClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackChapterClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.clients.TrackerMarkClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.GlobalSettingsProvider
import dev.brahmkshatriya.echo.common.providers.LyricsExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MessageFlowProvider
import dev.brahmkshatriya.echo.common.providers.MetadataProvider
import dev.brahmkshatriya.echo.common.providers.MiscExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.NetworkConnectionProvider
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
 *
 * ### Feed
 * To show feed of media items on main screen, the extension should implement the following clients:
 * - [HomeFeedClient] - To load the feed on the Home Tab
 * - [SearchFeedClient]/[QuickSearchClient] - To load the feed on the Search Tab
 * - [LibraryFeedClient] - To load the feed on the Library Tab
 *
 * ### Track Streaming
 * When streaming a track, the extension can implement the following clients:
 * - [TrackClient] - Mandatory to stream tracks
 * - [TrackChapterClient] - To mark tracks as played
 *
 * ### Media Items
 * To load media items, the extension should implement the following clients:
 * - [AlbumClient] - To load albums
 * - [PlaylistClient] - To load playlists
 * - [ArtistClient] - To load artists
 * - [RadioClient] - To load next tracks for the current track & show to radio button on media items with [EchoMediaItem.isRadioSupported] set to true
 *
 * ### Library
 * To allow library functionality, The extension can implement the following clients:
 * - [FollowClient] - To follow/unfollow items with [EchoMediaItem.isFollowable] set to true
 * - [SaveClient] - To save media items with [EchoMediaItem.isSaveable] set to true
 * - [LikeClient] - To like/unlike items with [EchoMediaItem.isLikeable] set to true
 * - [HideClient] - To hide items with [EchoMediaItem.isHideable] set to true
 * - [ShareClient] - To share items with [EchoMediaItem.isShareable] set to true
 *
 * ### Tracking and Lyrics
 * - [TrackerClient] - For tracking what the user is listening to
 * - [TrackerMarkClient] - For marking tracks as played
 * - [LyricsClient] - For lyrics support
 * - [LyricsSearchClient] - For searching lyrics using user query
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
 * - [MessageFlowProvider] - To send popup messages in the app
 * - [MusicExtensionsProvider] - To get installed music extensions
 * - [LyricsExtensionsProvider] - To get installed lyrics extensions
 * - [TrackerExtensionsProvider] - To get installed tracker extensions
 * - [MiscExtensionsProvider] - To get installed misc extensions
 * - [GlobalSettingsProvider] - To get global settings of the app
 * - [NetworkConnectionProvider] - To get network connection status
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
 * - [TrackerMarkClient] - For marking tracks as played
 * - [LoginClient] - For login support
 *
 * The extension can also implement the following providers:
 * - [MetadataProvider] - To get metadata of the extension
 * - [MessageFlowProvider] - To send messages in the app
 * - [MusicExtensionsProvider] - To get installed music extensions
 * - [LyricsExtensionsProvider] - To get installed lyrics extensions
 * - [TrackerExtensionsProvider] - To get installed tracker extensions
 * - [MiscExtensionsProvider] - To get installed misc extensions
 * - [GlobalSettingsProvider] - To get global settings of the app
 * - [NetworkConnectionProvider] - To get network connection status
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
 * - [GlobalSettingsProvider] - To get global settings of the app
 * - [NetworkConnectionProvider] - To get network connection status
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
 * - [GlobalSettingsProvider] - To get global settings of the app
 * - [NetworkConnectionProvider] - To get network connection status
 *
 * @param metadata The metadata of the extension
 * @param instance An injectable instance of the [ExtensionClient] client
 */
data class MiscExtension(
    override val metadata: Metadata,
    override val instance: Injectable<ExtensionClient>
) : Extension<ExtensionClient>(metadata, instance)