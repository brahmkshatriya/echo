package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData
import kotlinx.serialization.Serializable

/**
 * Represents a feed of multiple [T] items, Used in various contexts like home, library, etc.
 *
 * ### Feeds without Tabs
 * - The simplest way to create a feed is to show a list of [T] items, you can just do
 * the following to convert a list of [T] items to a Feed:
 * ```
 * val feed = listOf<T>().toFeed()
 * ```
 *
 * - If you want to show a feed that loads more stuff when scrolled to end, you need to use
 * the [PagedData] class. This class allows you to load more items as the user scrolls.
 * And You can use the following to convert a [PagedData] to a feed:
 * ```
 * val feed = pagedData.toFeed()
 * ```
 *
 * ### Feeds with Tabs
 * Feeds can have multiple [Tab] items, each representing a different category. When a tab is
 * selected, the [getPagedData] is called with the selected tab to retrieve a pair of [PagedData]
 * and [Buttons]. If the [tabs] list is empty, the getPagedData will be called with `null`.
 * An example of a feed with tabs:
 * ```
 * val feed = Feed(
 *      listOf("Tab1", "Tab2").map { Tab(it, it) }
 *  ) { tab ->
 *      val pagedData = when (tab?.id) {
 *          "Tab1" -> loadPagedDataForTab1()
 *          "Tab2" -> loadPagedDataForTab2()
 *          else -> throw IllegalArgumentException("Unknown tab")
 *      }
 *      pagedData.toFeedData()
 *  }
 * ```
 *
 * ### Buttons And Background
 * Feeds can also have [Buttons] shown below the tabs. Echo automatically handles searching and
 * filtering depending on the data provided in the items. You can provide a custom track list to
 * be used when play/shuffle button is clicked. You can send the buttons as `null`, if you want
 * echo to use the default buttons for the feed. Use the [Buttons.EMPTY] to force a feed to have
 * no buttons.
 *
 * An example of converting [PagedData] to [Feed.Data] with [Buttons]:
 * ```
 * val feedData = pagedData.toFeedData(
 *     buttons = Feed.Buttons(showPlayAndShuffle = true),
 *     background = "https://example.com/background.jpg".toImageHolder()
 * )
 * ```
 *
 * @property tabs The list of tabs in the feed.
 * @property getPagedData to retrieve the shelves with [Buttons] for a given tab
 *
 * @see Tab
 * @see PagedData
 * @see Buttons
 */
data class Feed<T : Any>(
    val tabs: List<Tab>,
    val getPagedData: suspend (Tab?) -> Data<T>
) {

    /**
     * A list of [Tab] items that are not sort tabs.
     * These tabs are used to load data in the feed and are not considered for sorting.
     **/
    val notSortTabs = tabs.filterNot { it.isSort }

    /**
     * Represents the loaded data of the [Feed].
     *
     * @property pagedData The [PagedData] containing the items for the feed.
     * @property buttons The [Buttons] to be shown in the feed. If `null`, the buttons will be decided automatically.
     * @property background The [ImageHolder] to be used as the background of the feed. If `null`, the background will be decided automatically.
     * */
    data class Data<T : Any>(
        val pagedData: PagedData<T>,
        val buttons: Buttons? = null,
        val background: ImageHolder? = null
    )

    /**
     * A data class representing the buttons that can be shown in the feed.
     *
     * @property showSearch Whether to show the search button.
     * @property showSort Whether to show the sort button.
     * @property showPlayAndShuffle Whether to show the play and shuffle buttons.
     * @property customTrackList To play a custom list of tracks when play and shuffle buttons are clicked.
     */
    @Serializable
    data class Buttons(
        val showSearch: Boolean = true,
        val showSort: Boolean = true,
        val showPlayAndShuffle: Boolean = false,
        val customTrackList: List<Track>? = null,
    ) {
        companion object {
            val EMPTY = Buttons(
                showSearch = false,
                showSort = false,
                showPlayAndShuffle = false,
                customTrackList = null
            )
        }
    }

    companion object {

        /**
         * Convenience function to convert a [PagedData] to a [Feed.Data].
         */
        fun <T : Any> PagedData<T>.toFeedData(
            buttons: Buttons? = null, background: ImageHolder? = null
        ) = Data(this, buttons, background)

        /**
         * Convenience function to create a [Feed] from a [PagedData] of [T] items.
         */
        fun <T : Any> PagedData<T>.toFeed(
            buttons: Buttons? = null, background: ImageHolder? = null
        ) = Feed(listOf()) { toFeedData(buttons, background) }

        /**
         * Convenience function to convert a list of [T] items to a [Feed.Data].
         */
        fun <T : Any> List<T>.toFeedData(
            buttons: Buttons? = null, background: ImageHolder? = null
        ) = Data(PagedData.Single { this }, buttons, background)

        /**
         * Convenience function to create a [Feed] from a list of [T] items.
         */
        fun <T : Any> List<T>.toFeed(
            buttons: Buttons? = null, background: ImageHolder? = null
        ) = Feed(listOf()) { PagedData.Single { this }.toFeedData(buttons, background) }

        /**
         * Convenience function to load all items in the [Feed].
         * Please use sparringly.
         */
        suspend fun <T : Any> Feed<T>.loadAll() = run {
            if (tabs.isEmpty()) return@run pagedDataOfFirst().loadAll()
            notSortTabs.flatMap { getPagedData(it).pagedData.loadAll() }
        }

        /**
         * Convenience function to load all items in the [Feed] for the firstOrNull [Tab].
         */
        suspend fun <T : Any> Feed<T>.pagedDataOfFirst() = run {
            getPagedData(notSortTabs.firstOrNull()).pagedData
        }
    }
}