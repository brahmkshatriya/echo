package dev.brahmkshatriya.echo.extensions

import android.content.SharedPreferences
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class Extensions(
    settings: SharedPreferences,
    private val scope: CoroutineScope,
    private val throwableFlow: MutableSharedFlow<Throwable>
) {
    fun flush() {
        music.value = null
        tracker.value = null
        lyrics.value = null
        misc.value = null
        all.value = null
        System.gc()
    }

    @Suppress("UNCHECKED_CAST")
    fun addAll(list: List<Result<Pair<Metadata, Injectable<ExtensionClient>>>>) {
        val musicList = mutableListOf<MusicExtension>()
        val trackerList = mutableListOf<TrackerExtension>()
        val lyricsList = mutableListOf<LyricsExtension>()
        val miscList = mutableListOf<MiscExtension>()
        val allList = mutableListOf<Extension<*>>()

        list.forEach { result ->
            val (metadata, injectable) = result.getOrElse {
                scope.launch { throwableFlow.emit(it) }
                return@forEach
            }
            val extension = when (metadata.type) {
                ExtensionType.MUSIC ->
                    MusicExtension(metadata, injectable)
                        .also { musicList.add(it) }

                ExtensionType.TRACKER ->
                    TrackerExtension(metadata, injectable as Injectable<TrackerClient>)
                        .also { trackerList.add(it) }

                ExtensionType.LYRICS ->
                    LyricsExtension(metadata, injectable as Injectable<LyricsClient>)
                        .also { lyricsList.add(it) }

                ExtensionType.MISC ->
                    MiscExtension(metadata, injectable)
                        .also { miscList.add(it) }
            }
            allList.add(extension)
        }

        music.value = musicList.sorted(ExtensionType.MUSIC)
        tracker.value = trackerList.sorted(ExtensionType.TRACKER)
        lyrics.value = lyricsList.sorted(ExtensionType.LYRICS)
        misc.value = miscList.sorted(ExtensionType.MISC)
        all.value = allList
    }

    val all = MutableStateFlow<List<Extension<*>>?>(null)

    val music = MutableStateFlow<List<MusicExtension>?>(null)
    val tracker = MutableStateFlow<List<TrackerExtension>?>(null)
    val lyrics = MutableStateFlow<List<LyricsExtension>?>(null)
    val misc = MutableStateFlow<List<MiscExtension>?>(null)
    val combined = combine(music, tracker, lyrics, misc) { a, b, c, d -> a + b + c + d }

    val priorityMap = ExtensionType.entries.associateWith {
        val key = it.priorityKey()
        val list = settings.getString(key, null).orEmpty().split(',')
        MutableStateFlow(list)
    }

    private fun <T : Extension<*>> List<T>.sorted(type: ExtensionType): List<T> {
        val priority = priorityMap[type]!!.value
        return sortedBy { priority.indexOf(it.metadata.id) }
    }

    init {
        priorityMap.forEach { (type, flow) ->
            scope.launch {
                flow.collectLatest {
                    when (type) {
                        ExtensionType.MUSIC -> music.value = music.value?.sorted(type)
                        ExtensionType.TRACKER -> tracker.value = tracker.value?.sorted(type)
                        ExtensionType.LYRICS -> lyrics.value = lyrics.value?.sorted(type)
                        ExtensionType.MISC -> misc.value = misc.value?.sorted(type)
                    }
                }
            }
        }
    }

    companion object {

        private operator fun <T> List<T>?.plus(other: List<T>?): List<T> {
            return buildList {
                this@plus?.let { addAll(it) }
                other?.let { addAll(it) }
            }
        }

        fun ExtensionType.priorityKey() = "priority_${this.feature}"
    }
}