package dev.brahmkshatriya.echo.app.playback

import com.skydoves.landscapist.core.Landscapist
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** One instance per application. Views and platform services share its session. */
class PlaybackComponent(
    dispatcher: CoroutineDispatcher,
    val imageLoader: Landscapist,
    createSystemMediaSession: (PlayerSession) -> SystemMediaSession,
    seedQueues: () -> List<QueueSeed> = ::demoQueueSeeds,
    createAudioOutputs: (CoroutineScope) -> AudioOutputs,
) : AutoCloseable {
    private var closed = false
    private val sessionDelegate = lazy {
        PlayerSession(seedQueues, dispatcher, createSystemMediaSession, createAudioOutputs)
    }

    val session: PlayerSession
        get() {
            check(!closed) { "Playback component is closed" }
            return sessionDelegate.value
        }

    val notification
        get() = session.systemMedia.notification

    override fun close() {
        if (closed) return
        closed = true
        closeAll(
            { if (sessionDelegate.isInitialized()) sessionDelegate.value.close() },
            { imageLoader.cancelAll() },
            { imageLoader.clearMemoryCache() },
        )
    }
}
