package dev.brahmkshatriya.echo.ui.media.adapter

import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.serialization.Serializable

enum class TrackSort(
    val title: Int,
    val sorter: (List<Track>) -> List<Track>
) {
    Title(R.string.sort_title, { list -> list.sortedBy { it.title } }),
    Duration(R.string.sort_duration, { list ->
        list.sortedBy { it.duration }
    }),
    Date(R.string.sort_date, { list ->
        list.sortedBy { it.releaseDate }
            .map { it.copy(subtitle = it.releaseDate.toString()) }
    }),
    Album(R.string.sort_album, { list ->
        list.sortedBy { it.album?.title ?: "" }
    }),
    Artist(R.string.sort_artist, { list ->
        list.sortedBy { it.artists.firstOrNull()?.name ?: "" }
    }),
    Plays(R.string.sort_plays, { list ->
        list.sortedBy { it.plays }
    }),
    Explicit(R.string.sort_explicit, { list ->
        list.sortedBy { it.isExplicit }
    });

    fun shouldBeVisible(data: List<Track>): TrackSort? {
        val take = when (this) {
            Title -> data.any { it.title.isNotEmpty() }
            Date -> data.any { it.releaseDate != null }
            Duration -> data.any { it.duration != null }
            Album -> data.notSame { it.album?.id } && data.any { it.album?.title != null }
            Artist -> data.notSame { it.artists.firstOrNull()?.id } && data.any { it.artists.firstOrNull()?.name != null }
            Plays -> data.any { it.plays != null }
            Explicit -> data.notSame { it.isExplicit }
        }
        return if (take) this else null
    }

    private fun List<Track>.notSame(selector: (Track) -> Any?): Boolean {
        return map(selector).distinct().size > 1
    }

    @Serializable
    data class State(
        val trackSort: TrackSort? = null,
        val reversed: Boolean = false,
        val save: Boolean = false,
    )

    companion object {
        fun getSorts(data: List<Track>): List<TrackSort> {
            return entries.filter { it.shouldBeVisible(data) != null }
        }
    }
}