package dev.brahmkshatriya.echo.ui.media

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.extensions.cache.Cached.getFeed
import dev.brahmkshatriya.echo.extensions.cache.Cached.getTracks
import dev.brahmkshatriya.echo.extensions.cache.Cached.loadFeed
import dev.brahmkshatriya.echo.extensions.cache.Cached.loadItem
import dev.brahmkshatriya.echo.extensions.cache.Cached.loadTracks
import dev.brahmkshatriya.echo.ui.feed.FeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.combine
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
    val cacheResultFlow = MutableStateFlow<Result<MediaState.Loaded<*>>?>(null)
    val itemResultFlow = MutableStateFlow<Result<MediaState.Loaded<*>>?>(null)

    val uiResultFlow = itemResultFlow.combine(cacheResultFlow) { item, cache ->
        item ?: cache
    }.stateIn(viewModelScope, Eagerly, null)

    val extensionItemFlow = itemResultFlow.map { result ->
        extensionFlow.value to result?.getOrNull()?.item
    }.stateIn(viewModelScope, Eagerly, null to null)

    private fun trackFeed(item: EchoMediaItem, extension: Extension<*>) =
        if (item is Track) runCatching {
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
                            item.artists.map {
                                loadItem(extension, it).getOrNull() ?: it
                            },
                        )
                    )
                },
            ).toFeed(Feed.Buttons.EMPTY)
        } else null

    val trackCachedFlow = extensionItemFlow.transformLatest { (extension, item) ->
        emit(null)
        if (!loadFeeds) return@transformLatest
        extension ?: return@transformLatest
        val item = item ?: cacheResultFlow.value?.getOrNull()?.item ?: return@transformLatest
        val feed = trackFeed(item, extension)
            ?: getTracks(app, extension.id, item).map { feed -> feed?.map { it.toShelf() } }
        emit(feed.map {
            it ?: return@map null
            FeedData.State(extension.id, item, it)
        })
    }.stateIn(viewModelScope, Eagerly, null)

    val tracksLoadedFlow = extensionItemFlow.transformLatest { (extension, item) ->
        emit(null)
        if (!loadFeeds) return@transformLatest
        extension ?: return@transformLatest
        item ?: return@transformLatest
        val feed = trackFeed(item, extension)
            ?: loadTracks(app, extension, item).map { feed -> feed?.map { it.toShelf() } }
        emit(feed.map {
            it ?: return@map null
            FeedData.State(extension.id, item, it)
        })
    }.stateIn(viewModelScope, Eagerly, null)

    val feedCachedFlow = extensionItemFlow.transformLatest { (extension, item) ->
        emit(null)
        if (!loadFeeds) return@transformLatest
        extension ?: return@transformLatest
        val item = item ?: cacheResultFlow.value?.getOrNull()?.item ?: return@transformLatest
        val feed = getFeed(app, extension.id, item) ?: return@transformLatest
        emit(feed.map {
            FeedData.State(extension.id, item, it)
        })
    }.stateIn(viewModelScope, Eagerly, null)

    val feedLoadedFlow = extensionItemFlow.transformLatest { (extension, item) ->
        emit(null)
        if (!loadFeeds) return@transformLatest
        extension ?: return@transformLatest
        item ?: return@transformLatest
        val feed = loadFeed(app, extension, item)
        emit(feed.map {
            it ?: return@map null
            FeedData.State(extension.id, item, it)
        })
    }.stateIn(viewModelScope, Eagerly, null)

    fun refresh() = viewModelScope.launch {
        refreshFlow.emit(Unit)
    }

    abstract fun getItem(): Triple<String, EchoMediaItem, Boolean>?

    fun likeItem(liked: Boolean) = app.scope.launch {
        val item = itemResultFlow.value?.getOrNull()?.item ?: return@launch
        val extension = extensionFlow.value
        like(app, extension, item, liked)
        refresh()
    }

    fun hideItem(hidden: Boolean) = app.scope.launch {
        val item = itemResultFlow.value?.getOrNull()?.item ?: return@launch
        val extension = extensionFlow.value
        hide(app, extension, item, hidden)
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
    val isRefreshingFlow = itemResultFlow.map {
        isRefreshing
    }

    companion object {

        suspend fun notFound(app: App, id: Int) {
            val notFound = app.context.run { getString(R.string.no_x_found, getString(id)) }
            app.messageFlow.emit(Message(notFound))
        }

        suspend fun createMessage(app: App, message: Context.() -> String) {
            app.messageFlow.emit(Message(app.context.message()))
        }

        suspend fun like(
            app: App, extension: Extension<*>?, item: EchoMediaItem, like: Boolean,
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) {
                getString(
                    if (like) R.string.liking_x else R.string.unliking_x,
                    item.title
                )
            }
            val result = extension.getIf<LikeClient, Unit>(app.throwFlow) {
                likeItem(item, like)
            }
            if (result != null) createMessage(app) {
                getString(
                    if (like) R.string.liked_x else R.string.unliked_x, item.title
                )
            }
        }

        suspend fun hide(
            app: App, extension: Extension<*>?, item: EchoMediaItem, hide: Boolean,
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) {
                getString(
                    if (hide) R.string.hiding_x else R.string.unhiding_x,
                    item.title
                )
            }
            val result = extension.getIf<HideClient, Unit>(app.throwFlow) {
                hideItem(item, hide)
            }
            if (result != null) createMessage(app) {
                getString(
                    if (hide) R.string.hidden_x else R.string.unhidden_x,
                    item.title
                )
            }
        }

        suspend fun follow(
            app: App, extension: Extension<*>?, item: EchoMediaItem, follow: Boolean,
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
            app: App, extension: Extension<*>?, item: EchoMediaItem, save: Boolean,
        ) {
            val extension = extension ?: return notFound(app, R.string.extension)
            createMessage(app) {
                getString(
                    if (save) R.string.saving_x else R.string.removing_x,
                    item.title
                )
            }
            val result = extension.getIf<SaveClient, Unit>(app.throwFlow) {
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
            app: App, extension: Extension<*>?, item: EchoMediaItem,
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