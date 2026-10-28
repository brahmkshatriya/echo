package dev.brahmkshatriya.echo.app.playback

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.skydoves.landscapist.core.Landscapist

fun createPlaybackComponent(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
): PlaybackComponent {
    val imageLoader = Landscapist.builder().build()
    return PlaybackComponent(dispatcher, imageLoader, { session -> createIosSystemMediaSession(session, imageLoader) })
}
