package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kmedianotification.MediaPlatform
import dev.brahmkshatriya.kmedianotification.browser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.skydoves.landscapist.core.Landscapist

fun createPlaybackComponent(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
): PlaybackComponent {
    val imageLoader = Landscapist.builder().build()
    return PlaybackComponent(dispatcher, imageLoader, { session ->
        MediaNotificationAdapter(session, MediaPlatform.browser(), imageLoader)
    })
}
