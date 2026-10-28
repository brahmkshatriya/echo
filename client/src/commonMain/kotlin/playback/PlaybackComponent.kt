package dev.brahmkshatriya.echo.app.playback

import kotlinx.coroutines.CoroutineDispatcher
import com.skydoves.landscapist.core.Landscapist

/** One instance per application. Views and platform services share its session. */
class PlaybackComponent internal constructor(
    dispatcher: CoroutineDispatcher,
    val imageLoader: Landscapist,
    createSystemMediaSession: (PlayerSession) -> SystemMediaSession,
    seedQueues: () -> List<QueueSeed> = ::demoQueueSeeds,
) : AutoCloseable {
    private var closed = false
    private val sessionDelegate = lazy {
        PlayerSession(seedQueues, dispatcher, createSystemMediaSession)
    }

    val session: PlayerSession
        get() {
            check(!closed) { "Playback component is closed" }
            return sessionDelegate.value
        }

    internal val notification get() = session.systemMedia.notification

    override fun close() {
        if (closed) return
        closed = true
        if (sessionDelegate.isInitialized()) sessionDelegate.value.close()
        imageLoader.cancelAll()
        imageLoader.clearMemoryCache()
    }
}
