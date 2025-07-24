package dev.brahmkshatriya.echo.ui.media

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MediaDetailsViewModel(
    downloader: Downloader,
    private val app: App,
    private val loadFeeds: Boolean,
    extension: Flow<MusicExtension?>,
) : ViewModel() {
    val extensionFlow = extension.stateIn(viewModelScope, Eagerly, null)
    val downloadsFlow = downloader.flow

    val refreshFlow = MutableSharedFlow<Unit>()
    val itemResultFlow = MutableStateFlow<Result<MediaState>?>(null)

    val tracksFlow = itemResultFlow.transformLatest { result ->
        emit(null)
        if (!loadFeeds) return@transformLatest
        val extension = extensionFlow.value ?: return@transformLatest
        val (item) = result?.getOrElse {
            emit(Result.failure(it))
            null
        } ?: return@transformLatest
        val feed = if (item is Track) runCatching {
            PagedData.Concat(
                PagedData.Single {
                    val album = item.album?.let { loadItem(extension, it).getOrNull() ?: it }
                    listOfNotNull(album?.toShelf())
                },
                PagedData.Single {
                    if (item.artists.isEmpty()) return@Single emptyList()
                    listOf(
                        Shelf.Lists.Items(
                            item.id + "_artists",
                            app.context.getString(R.string.artists),
                            item.artists,
                        )
                    )
                },
            ).toFeed()
        } else loadTracks(extension, item)?.map { feed -> feed?.map { it.toShelf() } }
        emit(feed)
    }.stateIn(viewModelScope, Eagerly, null)

    val feedFlow = itemResultFlow.transformLatest { result ->
        emit(null)
        if (!loadFeeds) return@transformLatest
        val extension = extensionFlow.value ?: return@transformLatest
        val (item) = result?.getOrElse {
            emit(Result.failure(it))
            null
        } ?: return@transformLatest
        emit(loadFeed(extension, item))
    }.stateIn(viewModelScope, Eagerly, null)

    fun refresh() = viewModelScope.launch { refreshFlow.emit(Unit) }
    abstract fun getItem(): Triple<String, EchoMediaItem, Boolean>?

    fun likeTrack(liked: Boolean) = app.scope.launch {
        val track = itemResultFlow.value?.getOrNull()?.item ?: return@launch
        val extension = extensionFlow.value
        like(app, extension, track, liked)
        refresh()
    }

    fun followItem(followed: Boolean) = app.scope.launch {
        val item = itemResultFlow.value?.getOrNull()?.item ?: return@launch
        val extension = extensionFlow.value
        follow(app, extension, item, followed)
        refresh()
    }

    fun saveToLibrary(saved: Boolean) = app.scope.launch {
        val item = itemResultFlow.value?.getOrNull()?.item ?: return@launch
        val extension = extensionFlow.value
        save(app, extension, item, saved)
        refresh()
    }

    fun onShare() = app.scope.launch(Dispatchers.IO) {
        val item = itemResultFlow.value?.getOrNull()?.item ?: return@launch
        val extension = extensionFlow.value
        share(app, extension, item)
    }

    val isRefreshing get() = itemResultFlow.value == null
    val isRefreshingFlow = itemResultFlow.map { isRefreshing }

    companion object {

        suspend fun loadMedia(
            extension: Extension<*>,
            item: EchoMediaItem,
            loaded: Boolean
        ) = runCatching {
            val new = if (loaded) item else loadItem(extension, item).getOrThrow()
            MediaState(
                item = new,
                extensionId = extension.id,
                isSaved = if (new.isSavable) extension.getIf<SaveToLibraryClient, Boolean> {
                    isSavedToLibrary(new)
                }.getOrThrow() else null,
                isFollowed = if (new.isFollowable) extension.getIf<FollowClient, Boolean> {
                    isFollowing(new)
                }.getOrThrow() else null,
                followers = if (new.isFollowable) extension.getIf<FollowClient, Long?> {
                    getFollowersCount(new)
                }.getOrThrow() else null,
                isLiked = if (new is Track) {
                    extension.getIf<TrackLikeClient, Boolean> { new.isLiked }.getOrThrow()
                } else null,
                showRadio = new.isRadioSupported && extension.isClient<RadioClient>(),
                showShare = new.isSharable && extension.isClient<ShareClient>(),
            )
        }

        suspend fun loadItem(extension: Extension<*>, item: EchoMediaItem) = when (item) {
            is Artist -> extension.getAs<ArtistClient, Artist> { loadArtist(item) }
            is Album -> extension.getAs<AlbumClient, Album> { loadAlbum(item) }
            is Playlist -> extension.getAs<PlaylistClient, Playlist> { loadPlaylist(item) }
            is Track -> extension.getAs<TrackClient, Track> { loadTrack(item, false) }
            is Radio -> extension.getAs<RadioClient, Radio> { loadRadio(item) }
        }

        suspend fun loadTracks(extension: Extension<*>, item: EchoMediaItem) = when (item) {
            is Album -> extension.getAs<AlbumClient, Feed<Track>?> { loadTracks(item) }
            is Playlist -> extension.getAs<PlaylistClient, Feed<Track>> { loadTracks(item) }
            is Radio -> extension.getAs<RadioClient, Feed<Track>> { loadTracks(item) }
            is Artist -> null
            is Track -> null
        }

        suspend fun loadFeed(extension: Extension<*>, item: EchoMediaItem) = when (item) {
            is Artist -> extension.getAs<ArtistClient, Feed<Shelf>> { loadFeed(item) }
            is Album -> extension.getAs<AlbumClient, Feed<Shelf>?> { loadFeed(item) }
            is Playlist -> extension.getAs<PlaylistClient, Feed<Shelf>?> { loadFeed(item) }
            is Track -> extension.getAs<TrackClient, Feed<Shelf>?> { loadFeed(item) }
            is Radio -> null
        }

        suspend fun notFound(app: App, id: Int) {
            val notFound = app.context.run { getString(R.string.no_x_found, getString(id)) }
            app.messageFlow.emit(Message(notFound))
        }

        suspend fun createMessage(app: App, message: Context.() -> String) {
            app.messageFlow.emit(Message(app.context.message()))
        }

        suspend fun like(
            app: App, extension: Extension<*>?, item: EchoMediaItem, like: Boolean
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) {
                getString(
                    if (like) R.string.liking_x else R.string.unliking_x,
                    item.title
                )
            }
            val result = extension.getIf<TrackLikeClient, Unit>(app.throwFlow) {
                likeTrack(item as Track, like)
            }
            if (result != null) createMessage(app) {
                getString(
                    if (like) R.string.liked_x else R.string.unliked_x, item.title
                )
            }
        }

        suspend fun follow(
            app: App, extension: Extension<*>?, item: EchoMediaItem, follow: Boolean
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) {
                getString(
                    if (follow) R.string.following_x else R.string.unfollowing_x,
                    item.title
                )
            }
            val result = extension.getIf<FollowClient, Unit>(app.throwFlow) {
                followItem(item, follow)
            }
            if (result != null) createMessage(app) {
                getString(
                    if (follow) R.string.followed_x else R.string.unfollowed_x,
                    item.title
                )
            }
        }

        suspend fun save(
            app: App, extension: Extension<*>?, item: EchoMediaItem, save: Boolean
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) {
                getString(
                    if (save) R.string.saving_x else R.string.removing_x,
                    item.title
                )
            }
            val result = extension.getIf<SaveToLibraryClient, Unit>(app.throwFlow) {
                saveToLibrary(item, save)
            }
            if (result != null) createMessage(app) {
                getString(
                    if (save) R.string.saved_x_to_library else R.string.removed_x_from_library,
                    item.title
                )
            }
        }

        suspend fun share(
            app: App, extension: Extension<*>?, item: EchoMediaItem
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) { getString(R.string.sharing_x, item.title) }
            val url = extension.getIf<ShareClient, String>(app.throwFlow) {
                onShare(item)
            } ?: return notFound(app, R.string.extension)
            val intent = ShareCompat.IntentBuilder(app.context)
                .setType("text/plain")
                .setChooserTitle("${extension.name} - ${item.title}")
                .setText(url)
                .createChooserIntent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.context.startActivity(intent)
        }

        private fun <T : Any> Feed<T>.map(transform: (T) -> Shelf) = Feed(tabs) { tab ->
            val data = getPagedData.invoke(tab)
            Feed.Data(
                data.pagedData.map {
                    val list = it.getOrThrow()
                    list.map(transform)
                },
                data.buttons,
                data.background
            )
        }
    }
}