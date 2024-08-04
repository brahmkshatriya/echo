package dev.brahmkshatriya.echo.ui.player.lyrics

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyric
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.plugger.ExtensionInfo
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.extension.ClientSelectionViewModel
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.utils.mapState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val settings: SharedPreferences,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    private val currentMediaFlow: MutableStateFlow<Current?>,
    throwableFlow: MutableSharedFlow<Throwable>
) : ClientSelectionViewModel(throwableFlow) {

    private val lyricsExtensionList = MutableStateFlow<List<LyricsExtension>>(emptyList())
    val currentExtension = MutableStateFlow<LyricsExtension?>(null)

    override val metadataFlow = lyricsExtensionList.mapState {
        it.map { lyricsExtension -> lyricsExtension.metadata }
    }
    override val currentFlow = currentExtension.mapState { it?.metadata?.id }

    override fun onClientSelected(clientId: String) {
        onLyricsClientSelected(lyricsExtensionList.getExtension(clientId))
    }

    private suspend fun update() {
        extensionListFlow.first { it != null }
        val trackExtension = currentMediaFlow.value?.mediaItem?.clientId?.let { id ->
            val extension = extensionListFlow.getExtension(id)
            val client = extension?.client
            if (client !is LyricsClient) return@let null
            LyricsExtension(extension.metadata, client, extension.info)
        }
        lyricsExtensionList.value =
            listOfNotNull(trackExtension) + lyricsListFlow.value.orEmpty()

        val id = settings.getString(LAST_LYRICS_KEY, null)
        val extension = lyricsExtensionList.value.find {
            it.metadata.id == id
        } ?: trackExtension
        onLyricsClientSelected(extension)
    }

    override fun onInitialize() {
        viewModelScope.launch {
            update()
            currentMediaFlow.collect {
                update()
            }
        }
    }

    private fun onLyricsClientSelected(extension: LyricsExtension?) {
        currentExtension.value = extension
        currentLyrics.value = null
        settings.edit().putString(LAST_LYRICS_KEY, extension?.info?.id).apply()
        val streamableTrack = currentMediaFlow.value?.mediaItem ?: return
        extension ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val data = onTrackChange(extension.info, streamableTrack)
            if (data != null) {
                currentLyrics.value = tryWith(extension.info) { data.loadFirst().firstOrNull() }
                data.toFlow().collectTo(searchResults)
            } else searchResults.value = null
        }
    }

    companion object {
        const val LAST_LYRICS_KEY = "last_lyrics_client"
    }


    val searchResults = MutableStateFlow<PagingData<Lyrics>?>(null)
    private suspend fun onSearch(info: ExtensionInfo, query: String?): PagedData<Lyrics>? {
        val client = currentExtension.value?.client
        if (client !is LyricsSearchClient) return null
        if (query == null) {
            return null
        }
        return tryWith(info) { client.searchLyrics(query) }
    }

    private suspend fun onTrackChange(
        info: ExtensionInfo,
        mediaItem: MediaItem
    ): PagedData<Lyrics>? {
        val client = currentExtension.value?.client ?: return null
        val track = mediaItem.track
        return tryWith(info) { client.searchTrackLyrics(mediaItem.clientId, track) }
    }

    fun search(query: String?) {
        val info = currentExtension.value?.info ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val data = onSearch(info, query)
            if (data != null) data.toFlow().collectTo(searchResults)
            else searchResults.value = null
        }
    }

    val loading = MutableStateFlow(false)
    val currentLyrics = MutableStateFlow<Lyrics?>(null)
    fun onLyricsSelected(lyricsItem: Lyrics) {
        loading.value = true
        currentLyrics.value = lyricsItem
        val extension = currentExtension.value ?: return
        val client = extension.client
        val info = extension.info
        viewModelScope.launch(Dispatchers.IO) {
            currentLyrics.value = tryWith(info) { client.loadLyrics(lyricsItem) }?.fillGaps()
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
