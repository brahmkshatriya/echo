package dev.brahmkshatriya.echo.ui.lyrics

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyric
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.extension.ClientSelectionViewModel
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.utils.mapState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val global: Queue,
    private val settings: SharedPreferences,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    throwableFlow: MutableSharedFlow<Throwable>
) : ClientSelectionViewModel(throwableFlow) {

    private val lyricsExtensionList = MutableStateFlow<List<LyricsExtension>>(emptyList())
    val currentExtension = MutableStateFlow<LyricsExtension?>(null)

    override val metadataFlow = lyricsExtensionList.mapState {
        it.map { lyricsExtension -> lyricsExtension.metadata }
    }
    override val currentFlow = currentExtension.mapState { it?.metadata?.id }

    override fun onClientSelected(clientId: String) {
        println("$clientId selected")
        onLyricsClientSelected(lyricsExtensionList.getExtension(clientId))
    }

    private suspend fun update(){
        extensionListFlow.first { it != null }
        val trackExtension = global.current?.clientId?.let { id ->
            val extension = extensionListFlow.getExtension(id)
            val client = extension?.client
            if (client !is LyricsClient) return@let null
            LyricsExtension(extension.metadata, client)
        }
        lyricsExtensionList.value =
            listOfNotNull(trackExtension) + lyricsListFlow.value.orEmpty()

        val id = settings.getString(LAST_LYRICS_KEY, null)
        val extension = lyricsExtensionList.value.find {
            it.metadata.id == id
        } ?: trackExtension
        println("update called")
        onLyricsClientSelected(extension)
    }

    override fun onInitialize() {
        viewModelScope.launch {
            update()
            global.currentIndexFlow.map { global.current }.distinctUntilChanged().collect { _ ->
                println("current changed")
                update()
            }
        }
    }

    private fun onLyricsClientSelected(extension: LyricsExtension?) {
        currentExtension.value = extension
        println("onLyricsClientSelected: $extension")
        currentLyrics.value = null
        settings.edit().putString(LAST_LYRICS_KEY, extension?.metadata?.id).apply()
        val streamableTrack = global.current ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val data = onTrackChange(streamableTrack)
            if (data != null) {
                currentLyrics.value = data.loadFirst().firstOrNull()
                data.toFlow().collectTo(searchResults)
            } else searchResults.value = null
        }
    }

    companion object {
        const val LAST_LYRICS_KEY = "last_lyrics_client"
    }


    val searchResults = MutableStateFlow<PagingData<Lyrics>?>(null)
    private suspend fun onSearch(query: String?): PagedData<Lyrics>? {
        val client = currentExtension.value?.client
        if (client !is LyricsSearchClient) return null
        if (query == null) {
            return null
        }
        return tryWith { client.searchLyrics(query) }
    }

    private suspend fun onTrackChange(streamableTrack: Queue.StreamableTrack): PagedData<Lyrics>? {
        val client = currentExtension.value?.client ?: return null
        val track = streamableTrack.loaded ?: streamableTrack.onLoad.first()
        return tryWith { client.searchTrackLyrics(streamableTrack.clientId, track) }
    }

    fun search(query: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = onSearch(query)
            if (data != null) data.toFlow().collectTo(searchResults)
            else searchResults.value = null
        }
    }

    val loading = MutableStateFlow(false)
    val currentLyrics = MutableStateFlow<Lyrics?>(null)
    fun onLyricsSelected(lyricsItem: Lyrics) {
        loading.value = true
        currentLyrics.value = lyricsItem
        val client = currentExtension.value?.client ?: return
        viewModelScope.launch(Dispatchers.IO) {
            currentLyrics.value = tryWith { client.loadLyrics(lyricsItem) }?.fillGaps()
            loading.value = false
        }
    }

    private fun Lyrics.fillGaps(): Lyrics {
        val lyrics = this.lyrics
        return if (fillTimeGaps && lyrics != null) {
            val new = mutableListOf<Lyric>()
            var last = 0L
            lyrics.forEach {
                if (it.startTime > last) {
                    new.add(Lyric("", last, it.startTime))
                }
                new.add(it)
                last = it.endTime
            }
            this.copy(lyrics = new)
        } else this
    }
}
