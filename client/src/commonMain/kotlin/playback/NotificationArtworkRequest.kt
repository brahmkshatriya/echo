package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kmedianotification.MediaArtwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Lives with playback rather than the composition, so background media controls retain artwork. */
internal class NotificationArtworkRequest(
    private val scope: CoroutineScope,
    private val load: suspend (String) -> MediaArtwork,
    private val onLoaded: (String, MediaArtwork) -> Unit,
    private val onError: (Throwable) -> Unit,
) : AutoCloseable {
    var source: String? = null
        private set
    var artwork: MediaArtwork? = null
        private set
    private var request: Job? = null
    private var closed = false

    fun update(model: String?, retry: Boolean = false) {
        if (closed || model == source && !(retry && artwork == null && request?.isActive != true)) return
        request?.cancel()
        request = null
        source = model
        artwork = null
        if (model == null) return
        request = scope.launch {
            try {
                val loaded = load(model)
                coroutineContext.ensureActive()
                if (!closed && source == model) {
                    artwork = loaded
                    onLoaded(model, loaded)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (!closed && source == model) onError(failure)
            }
        }
    }

    override fun close() {
        closed = true
        request?.cancel()
        request = null
        source = null
        artwork = null
    }
}
