package dev.brahmkshatriya.echo.ui.player.lyrics

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyric
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.extensions.run
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.extension.ClientSelectionViewModel
import dev.brahmkshatriya.echo.ui.paging.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tel.jeelpa.plugger.utils.mapState
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val settings: SharedPreferences,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    private val currentMediaFlow: MutableStateFlow<Current?>,
    throwableFlow: MutableSharedFlow<Throwable>
) : ClientSelectionViewModel(throwableFlow) {

    private val lyricsExtensionList = MutableStateFlow<List<Extension<*>>>(emptyList())
    val currentExtension = MutableStateFlow<Extension<*>?>(null)

    override val metadataFlow = lyricsExtensionList.mapState {
        it.map { lyricsExtension -> lyricsExtension.metadata }
    }
    override val currentFlow = currentExtension.mapState { it?.metadata?.id }

    override fun onClientSelected(clientId: String) {
        onLyricsClientSelected(lyricsExtensionList.getExtension(clientId))
    }

    private fun update() {
        val trackExtension = currentMediaFlow.value?.mediaItem?.clientId?.let { id ->
            extensionListFlow.getExtension(id)?.takeIf { it.isClient<LyricsClient>() }
        }
        lyricsExtensionList.value =
            listOfNotNull(trackExtension) + lyricsListFlow.value.orEmpty()

        val id = settings.getString(LAST_LYRICS_KEY, null)
        val extension = lyricsExtensionList.getExtension(id) ?: trackExtension
        onLyricsClientSelected(extension)
    }

    override fun onInitialize() {
        update()
        viewModelScope.launch {
            currentMediaFlow.collect {
                update()
            }
        }
    }

    private fun onLyricsClientSelected(extension: Extension<*>?) {
        currentExtension.value = extension
        currentLyrics.value = null
        settings.edit().putString(LAST_LYRICS_KEY, extension?.id).apply()
        val streamableTrack = currentMediaFlow.value?.mediaItem ?: return
        extension ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val data = onTrackChange(extension, streamableTrack)
            if (data != null) {
                currentLyrics.value =
                    extension.run(throwableFlow) { data.loadFirst().firstOrNull() }
                data.toFlow().collectTo(searchResults)
            } else searchResults.value = null
        }
    }

    companion object {
        const val LAST_LYRICS_KEY = "last_lyrics_client"
    }


    val searchResults = MutableStateFlow<PagingData<Lyrics>?>(null)
    private suspend fun onSearch(query: String?): PagedData<Lyrics>? {
        val extension = currentExtension.value
        if (query == null) return null
        return extension?.get<LyricsSearchClient, PagedData<Lyrics>>(throwableFlow) {
            searchLyrics(query)
        }
    }

    private suspend fun onTrackChange(
        extension: Extension<*>,
        mediaItem: MediaItem
    ): PagedData<Lyrics>? {
        val track = mediaItem.track
        return extension.get<LyricsClient, PagedData<Lyrics>>(throwableFlow) {
            searchTrackLyrics(mediaItem.clientId, track)
        }
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
        val extension = currentExtension.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            currentLyrics.value = extension.get<LyricsClient, Lyrics>(throwableFlow) {
                loadLyrics(lyricsItem)
            }?.fillGaps()
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
