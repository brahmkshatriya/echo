package dev.brahmkshatriya.echo.extensions.cache

import com.mayakapps.kache.FileKache
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File

object Cached {
    class NotFound(id: String) : Exception("Cache not found for $id")

    suspend inline fun <reified T> FileKache.getData(id: String) = runCatching {
        val file = get(id) ?: throw NotFound(id)
        File(file).readText().toData<T>()
    }

    suspend inline fun <reified T> FileKache.putData(id: String, data: T) = runCatching {
        put(id) {
            runCatching {
                File(it).writeText(data.toJson())
            }.isSuccess
        }
    }

    suspend inline fun <reified T : EchoMediaItem> getMedia(
        app: App, extensionId: String, itemId: String,
    ) = runCatching {
        val fileCache = app.fileCache.await()
        val id = "media-$extensionId-$itemId-state"
        fileCache.getData<MediaState.Loaded<T>>(id).getOrThrow()
    }

    suspend inline fun <reified T : EchoMediaItem> loadMedia(
        app: App, extension: Extension<*>, state: MediaState<T>,
    ) = coroutineScope {
        runCatching {
            val result = runCatching {
                val new = loadItem(extension, state.item).getOrThrow()
                val isSaved = async {
                    if (new.isSaveable) extension.getIf<SaveClient, Boolean> {
                        isItemSaved(new)
                    } else null
                }
                val isFollowed = async {
                    if (new.isFollowable) extension.getIf<FollowClient, Boolean> {
                        isFollowing(new)
                    } else null
                }
                val followers = async {
                    if (new.isFollowable) extension.getIf<FollowClient, Long?> {
                        getFollowersCount(new)
                    } else null
                }
                val isLiked = async {
                    if (new.isLikeable) extension.getIf<LikeClient, Boolean> {
                        isItemLiked(new)
                    } else null
                }
                val isHidden = async {
                    if (new.isHideable) extension.getIf<HideClient, Boolean> {
                        isItemHidden(new)
                    } else null
                }
                val newState = MediaState.Loaded(
                    item = new,
                    extensionId = extension.id,
                    isSaved = isSaved.await()?.getOrThrow(),
                    isFollowed = isFollowed.await()?.getOrThrow(),
                    followers = followers.await()?.getOrThrow(),
                    isLiked = isLiked.await()?.getOrThrow(),
                    isHidden = isHidden.await()?.getOrThrow(),
                    showRadio = new.isRadioSupported && extension.isClient<RadioClient>(),
                    showShare = new.isShareable && extension.isClient<ShareClient>(),
                )
                val fileCache = app.fileCache.await()
                val id = "media-${extension.id}-${newState.item.id}-state"
                fileCache.putData(id, newState)
                newState
            }
            result.getOrElse {
                getMedia<T>(app, extension.id, state.item.id).getOrNull()
                    ?: throw it
            }
        }
    }

    suspend inline fun <reified T : EchoMediaItem> loadItem(
        extension: Extension<*>, item: T,
    ) = runCatching {
        when (item) {
            is Artist -> extension.getAs<ArtistClient, Artist> { loadArtist(item) }
            is Album -> extension.getAs<AlbumClient, Album> { loadAlbum(item) }
            is Playlist -> extension.getAs<PlaylistClient, Playlist> { loadPlaylist(item) }
            is Track -> extension.getAs<TrackClient, Track> { loadTrack(item, false) }
            is Radio -> extension.getAs<RadioClient, Radio> { loadRadio(item) }
        }.getOrThrow() as T
    }

    suspend fun loadStreamableMedia(
        app: App, extension: Extension<*>, track: Track, streamable: Streamable,
    ) = runCatching {
        val fileCache = app.fileCache.await()
        val id = "media-${extension.id}-${track.id}-${streamable.id}"
        val media = extension.getAs<TrackClient, Streamable.Media> {
            loadStreamableMedia(streamable, false)
        }.getOrElse { throwable ->
            fileCache.getData<Streamable.Media>(id).getOrNull() ?: throw throwable
        }
        fileCache.putData(id, media)
        media
    }

    suspend fun getTracks(
        app: App, extensionId: String, item: EchoMediaItem,
    ) = runCatching {
        if (item !is EchoMediaItem.Lists) return@runCatching null
        val itemId = item.id
        getFeed<Track>(app, extensionId, "$itemId-tracks") { it }
    }

    suspend fun loadTracks(app: App, extension: Extension<*>, item: EchoMediaItem) = runCatching {
        val feed = when (item) {
            is Album -> extension.getAs<AlbumClient, Feed<Track>?> { loadTracks(item) }
            is Playlist -> extension.getAs<PlaylistClient, Feed<Track>> { loadTracks(item) }
            is Radio -> extension.getAs<RadioClient, Feed<Track>> { loadTracks(item) }
            is Artist -> null
            is Track -> null
        }?.getOrThrow() ?: return@runCatching null
        savingFeed(app, extension, "${item.id}-tracks", feed)
    }

    suspend fun getFeed(
        app: App, extensionId: String, item: EchoMediaItem,
    ) = run {
        if (item !is EchoMediaItem.Lists) return@run null
        val itemId = item.id
        getFeedShelf(app, extensionId, itemId)
    }

    suspend fun loadFeed(app: App, extension: Extension<*>, item: EchoMediaItem) = runCatching {
        val feed = when (item) {
            is Artist -> extension.getAs<ArtistClient, Feed<Shelf>> { loadFeed(item) }
            is Album -> extension.getAs<AlbumClient, Feed<Shelf>?> { loadFeed(item) }
            is Playlist -> extension.getAs<PlaylistClient, Feed<Shelf>?> { loadFeed(item) }
            is Track -> extension.getAs<TrackClient, Feed<Shelf>?> { loadFeed(item) }
            is Radio -> null
        }?.getOrThrow() ?: return@runCatching null
        savingFeed(app, extension, item.id, feed)
    }

    suspend fun loadLyrics(app: App, extension: Extension<*>, lyrics: Lyrics) = runCatching {
        val fileCache = app.fileCache.await()
        val id = "lyrics-${extension.id}-${lyrics.id}"
        val loaded = extension.getAs<LyricsClient, Lyrics> {
            loadLyrics(lyrics)
        }.getOrElse { throwable ->
            fileCache.getData<Lyrics>(id).getOrNull() ?: throw throwable
        }
        fileCache.putData(id, loaded)
        loaded
    }

    suspend fun getLyricsFeed(
        app: App, extensionId: String, clientId: String, track: Track, query: String,
    ) = runCatching {
        val id = if (query.isEmpty()) "lyrics-$clientId-${track.id}" else "lyrics-search-$query"
        getFeed<Lyrics>(app, extensionId, id) { it }
    }

    suspend fun loadLyricsFeed(
        app: App, extension: Extension<*>, clientId: String, track: Track, query: String,
    ) = runCatching {
        val feed = if (query.isEmpty()) extension.getAs<LyricsClient, Feed<Lyrics>> {
            searchTrackLyrics(clientId, track)
        }.getOrThrow() else extension.getAs<LyricsSearchClient, Feed<Lyrics>> {
            searchLyrics(query)
        }.getOrThrow()
        val id = if (query.isEmpty()) "lyrics-$clientId-${track.id}" else "lyrics-search-$query"
        savingFeed(app, extension, id, feed)
    }

    suspend fun getFeedShelf(
        app: App, extensionId: String, feedId: String,
    ): Result<Feed<Shelf>> = runCatching {
        getFeed<Shelf>(app, extensionId, feedId) { shelf ->
            when (shelf) {
                is Shelf.Item -> shelf
                is Shelf.Category -> shelf.copy(
                    feed = getFeedShelf(app, extensionId, shelf.id).getOrNull()
                )

                is Shelf.Lists.Categories -> shelf.copy(
                    list = shelf.list.map {
                        it.copy(feed = getFeedShelf(app, extensionId, it.id).getOrNull())
                    },
                    more = getFeedShelf(app, extensionId, shelf.id).getOrNull()
                )

                is Shelf.Lists.Items -> shelf.copy(
                    more = getFeedShelf(app, extensionId, shelf.id).getOrNull()
                )

                is Shelf.Lists.Tracks -> shelf.copy(
                    more = getFeedShelf(app, extensionId, shelf.id).getOrNull()
                )
            }
        }
    }


    // FEED STUFF

    suspend inline fun <reified T : Any> getFeed(
        app: App, extensionId: String, feedId: String, crossinline transform: suspend (T) -> T,
    ): Feed<T> {
        val fileCache = app.fileCache.await()
        val tabId = "feed-$extensionId-$feedId"
        val tabs = fileCache.getData<List<Tab>>(tabId).getOrThrow()
        return Feed(tabs) { tab ->
            val id = "$tabId-${tab?.id}"
            val (buttons, bg) = fileCache.getData<Pair<Feed.Buttons?, ImageHolder?>>(id)
                .getOrThrow()
            PagedData.Continuous { token ->
                val id = "$id-$token"
                val page = fileCache.getData<Page<T>>(id).getOrThrow()
                page.copy(page.data.map { transform(it) }).also {
                    println("Got page $id with ${it.data.size} items" )
                }
            }.toFeedData(buttons, bg).also {
                println("Got feed data $id with $it" )
            }
        }.also {
            println("Got $tabId from cache with ${tabs.size} tabs")
        }
    }

    suspend inline fun <reified T : Any> savingFeed(
        app: App, extension: Extension<*>, feedId: String, feed: Feed<T>,
    ): Feed<T> {
        val fileCache = app.fileCache.await()
        val tabId = "feed-${extension.id}-$feedId"
        fileCache.putData(tabId, feed.tabs)
        return Feed(feed.tabs) { tab ->
            val data = runCatching {
                feed.getPagedData(tab)
            }.getOrElse {
                throw it.toAppException(extension)
            }
            val (pagedData, buttons, bg) = data
            val id = "$tabId-${tab?.id}"
            fileCache.putData(id, Pair(buttons, bg))
            PagedData.Continuous { token ->
                val page = runCatching {
                    pagedData.loadPage(token)
                }.getOrElse {
                    throw it.toAppException(extension)
                }
                val id = "$id-$token"
                fileCache.putData(id, page)
                page
            }.toFeedData(buttons, bg)
        }
    }

}