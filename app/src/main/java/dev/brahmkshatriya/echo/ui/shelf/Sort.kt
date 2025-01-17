package dev.brahmkshatriya.echo.ui.shelf

import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.utils.ui.toTimeString


enum class Sort(
    val title: Int,
    val sorter: (List<Shelf>) -> List<Shelf>
) {
    Title(R.string.sort_title, { list -> list.sortedBy { ShelfUtils.title(it) } }),
    Subtitle(R.string.sort_subtitle, { list -> list.sortedBy { ShelfUtils.subtitle(it) } }),
    Date(R.string.sort_date, { list ->
        list.map { it to ShelfUtils.date(it) }.filter { it.second.year != 0 }
            .sortedBy { it.second }
            .map { ShelfUtils.changeSubtitle(it.first, it.second.toString()) }
    }),
    Duration(R.string.sort_duration, { list ->
        list.map { it to ShelfUtils.duration(it) }.filter { it.second != 0L }
            .sortedBy { it.second }
            .map { ShelfUtils.changeSubtitle(it.first, it.second.toTimeString()) }
    });

    fun shouldBeVisible(data: List<Shelf>): Sort? {
        return if (data.any {
                when (this) {
                    Title -> ShelfUtils.title(it).isNotEmpty()
                    Subtitle -> ShelfUtils.subtitle(it).isNotEmpty()
                    Date -> ShelfUtils.date(it).year != 0
                    Duration -> ShelfUtils.duration(it) != 0L
                }
            }) this
        else null
    }

    companion object {
        fun getSorts(data: List<Shelf>): List<Sort> {
            return entries.filter { it.shouldBeVisible(data) != null }
        }
    }
}
