package dev.brahmkshatriya.echo.ui.player

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getClient
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val global: Queue,
    private val settings: SharedPreferences,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    val lyricsExtensionList = MutableStateFlow<List<LyricsExtension>>(emptyList())
    val currentExtension = MutableStateFlow<LyricsExtension?>(null)
    override fun onInitialize() {
        viewModelScope.launch {
            global.currentIndexFlow.collect {
                val trackExtension = global.current?.clientId?.let { id ->
                    val extension = extensionListFlow.getClient(id)
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
                onLyricsClientSelected(extension)
            }
        }
    }

    //TODO extend from ClientSelectionViewModel
    fun onLyricsClientSelected(extension: LyricsExtension?) {
        currentExtension.value = extension
        currentLyrics.value = null
        settings.edit().putString(LAST_LYRICS_KEY, extension?.metadata?.id).apply()
    }

    companion object {
        const val LAST_LYRICS_KEY = "last_lyrics_client"
    }


    val searchResults = MutableStateFlow<PagingData<Lyrics>?>(null)
    private suspend fun onSearch(query: String): PagedData<Lyrics>? {
        val client = currentExtension.value?.client
        if (client !is LyricsSearchClient) return null
        return tryWith { client.searchLyrics(query) }
    }

    private suspend fun onTrackChange(streamableTrack: Queue.StreamableTrack): PagedData<Lyrics>? {
        val client = currentExtension.value?.client ?: return null
        val track = streamableTrack.loaded ?: streamableTrack.onLoad.first()
        return tryWith { client.searchTrackLyrics(streamableTrack.clientId, track) }
    }

    fun trackChange(streamableTrack: Queue.StreamableTrack) {
        viewModelScope.launch {
            val data = onTrackChange(streamableTrack)
            if (data != null) data.toFlow().collectTo(searchResults)
            else searchResults.value = null
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            val data = onSearch(query)
            if (data != null) data.toFlow().collectTo(searchResults)
            else searchResults.value = null
        }
    }

    val currentLyrics = MutableStateFlow<Lyrics?>(null)
    fun onLyricsSelected(lyrics: Lyrics) {
        val client = currentExtension.value?.client ?: return
        viewModelScope.launch {
            currentLyrics.value = tryWith { client.loadLyrics(lyrics) }
        }
    }
}
