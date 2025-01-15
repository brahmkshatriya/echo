package dev.brahmkshatriya.echo.offline

import android.content.Context
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.ui.exception.AppException
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.getSettings
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.runBlocking

class UnifiedExtension(
    private val context: Context
) : ExtensionClient, MusicExtensionsProvider, HomeFeedClient,
    SearchFeedClient, TrackClient {
    companion object {
        private const val ID = "unified"
        private const val EXTENSION_ID = "extension_id"
        val metadata = Metadata(
            "UnifiedExtension",
            "",
            ImportType.BuiltIn,
            ID,
            "Unified Extension",
            "1.0.0",
            "Beta extension to test unified home feed",
            "Echo",
            enabled = false
        )

        inline fun <reified C, T> Extension<*>.client(block: C.() -> T): T = run {
            val client = runBlocking { instance.value().getOrThrow() } as? C
                ?: throw ClientException.NotSupported("$name - ${C::class.java.name}")
            runCatching { client.block() }
        }.getOrElse { throw AppException.Other(it, this) }

        private fun List<Extension<*>>.get(id: String?) =
            find { it.id == id } ?: throw Exception("Extension $id not found")

        private val Map<String, String>.extensionId
            get() = this[EXTENSION_ID] ?: throw Exception("Extension id not found")

        private fun Track.withExtensionId(id: String) = copy(
            extras = extras + mapOf(EXTENSION_ID to id),
            streamables = streamables.map { it.copy(extras = it.extras + mapOf(EXTENSION_ID to id)) }
        )

        private fun EchoMediaItem.withExtensionId(id: String): EchoMediaItem {
            return when (this) {
                is EchoMediaItem.Lists.AlbumItem -> album.copy(
                    extras = album.extras + mapOf(EXTENSION_ID to id)
                ).toMediaItem()

                is EchoMediaItem.Lists.PlaylistItem -> playlist.copy(
                    extras = playlist.extras + mapOf(EXTENSION_ID to id)
                ).toMediaItem()

                is EchoMediaItem.Lists.RadioItem -> radio.copy(
                    extras = radio.extras + mapOf(EXTENSION_ID to id)
                ).toMediaItem()

                is EchoMediaItem.Profile.ArtistItem -> artist.copy(
                    extras = artist.extras + mapOf(EXTENSION_ID to id)
                ).toMediaItem()

                is EchoMediaItem.Profile.UserItem -> user.copy(
                    extras = user.extras + mapOf(EXTENSION_ID to id)
                ).toMediaItem()

                is EchoMediaItem.TrackItem -> track.withExtensionId(id).toMediaItem()
            }
        }

        private fun Shelf.Item.withExtensionId(id: String) = copy(
            media = media.withExtensionId(id)
        )

        private fun Shelf.Category.withExtensionId(id: String) =
            copy(items = items?.let { injectExtensionId(id, it) })

        fun injectExtensionId(id: String, shelf: PagedData<Shelf>): PagedData<Shelf> =
            PagedData.Continuous {
                val list = shelf.loadNext()?.map {
                    when (it) {
                        is Shelf.Category -> it.withExtensionId(id)
                        is Shelf.Item -> it.withExtensionId(id)
                        is Shelf.Lists.Categories -> it.copy(
                            list = it.list.map { category -> category.withExtensionId(id) }
                        )

                        is Shelf.Lists.Items -> it.copy(
                            list = it.list.map { item -> item.withExtensionId(id) }
                        )

                        is Shelf.Lists.Tracks -> it.copy(
                            list = it.list.map { track -> track.withExtensionId(id) }
                        )
                    }
                } ?: emptyList()
                val hasMore = shelf.hasNext()
                Page(list, if (hasMore) list.hashCode().toString() else null)
            }
    }

    override suspend fun onExtensionSelected() {}
    override val settingItems = listOf(
        SettingSwitch(
            context.getString(R.string.show_tabs),
            "show_tabs",
            context.getString(R.string.show_tab_summary),
            true
        ),
    )

    private val settings = getSettings(context, ExtensionType.MUSIC, metadata)
    override fun setSettings(settings: Settings) {}
    private val showTabs get() = settings.getBoolean("show_tabs") ?: true

    override val requiredMusicExtensions = listOf<String>()
    private var extensions = listOf<MusicExtension>()
    override fun setMusicExtensions(extensions: List<MusicExtension>) {
        this.extensions = extensions.filter { it.id != ID }
    }

    private fun feed(
        id: String,
        loadTabs: suspend () -> List<Tab>,
        getFeed: (Tab?) -> PagedData<Shelf>
    ): PagedData.Continuous<Shelf> = PagedData.Continuous {
        val tabs = loadTabs()
        val shelf = if (!showTabs || tabs.isEmpty())
            injectExtensionId(id, getFeed(tabs.firstOrNull()))
        else PagedData.Single {
            listOf(
                Shelf.Lists.Categories(
                    context.getString(R.string.tabs),
                    tabs.map { Shelf.Category(it.title, injectExtensionId(id, getFeed(it))) },
                    type = Shelf.Lists.Type.Grid
                )
            )
        }
        val list = shelf.loadNext().orEmpty()
        val hasNext = shelf.hasNext()
        Page(list, if (hasNext) list.hashCode().toString() else null)
    }

    override suspend fun getHomeTabs() = extensions.map { Tab(it.id, it.name) }
    override fun getHomeFeed(tab: Tab?): PagedData<Shelf> {
        val id = tab?.id ?: return PagedData.empty()
        return extensions.get(id).client<HomeFeedClient, PagedData<Shelf>> {
            feed(id, { getHomeTabs() }, { getHomeFeed(it) })
        }
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        val history = getHistory().toMutableList()
        history.remove(item.title)
        context.saveToCache("search_history", history, "offline")
    }

    private fun getHistory() = context.getFromCache<List<String>>("search_history", "offline")
        ?.distinct()?.take(5)
        ?: emptyList()

    private fun saveInHistory(query: String) {
        val history = getHistory().toMutableList()
        history.add(0, query)
        context.saveToCache("search_history", history, "offline")
    }

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        return if (query.isBlank()) {
            getHistory().map { QuickSearchItem.Query(it, true) }
        } else listOf()
    }

    override suspend fun searchTabs(query: String) = extensions.map { Tab(it.id, it.name) }
    override fun searchFeed(query: String, tab: Tab?): PagedData<Shelf> {
        if (query.isNotBlank()) saveInHistory(query)

        val id = tab?.id ?: return PagedData.empty()
        return extensions.get(id).client<SearchFeedClient, PagedData<Shelf>> {
            feed(id, { searchTabs(query) }, { searchFeed(query, it) })
        }
    }

    override suspend fun loadTrack(track: Track): Track {
        val id = track.extras.extensionId
        return extensions.get(id).client<TrackClient, Track> {
            loadTrack(track).withExtensionId(id)
        }
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable, isDownload: Boolean
    ): Streamable.Media {
        val id = streamable.extras.extensionId
        return extensions.get(id).client<TrackClient, Streamable.Media> {
            loadStreamableMedia(streamable, isDownload)
        }
    }

    override fun getShelves(track: Track): PagedData<Shelf> {
        val id = track.extras.extensionId
        return extensions.get(id).client<TrackClient, PagedData<Shelf>> {
            injectExtensionId(id, getShelves(track))
        }
    }
}