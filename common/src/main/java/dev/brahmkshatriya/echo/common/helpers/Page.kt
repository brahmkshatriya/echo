package dev.brahmkshatriya.echo.common.helpers

/**
 * Represents a page of data, with a continuation token for pagination
 *
 * @param T The type of data items
 * @property data The list of data items
 * @property continuation The next continuation token for pagination. If null, there are no more pages
 */
data class Page<T : Any>(
    val data: List<T>,
    val continuation: String?
)