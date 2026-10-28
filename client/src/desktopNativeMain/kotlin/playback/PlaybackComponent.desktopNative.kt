package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kmedianotification.MediaPlatform
import dev.brahmkshatriya.kmedianotification.desktop
import kotlinx.coroutines.CoroutineDispatcher
import com.skydoves.landscapist.core.Landscapist

/** The desktop application supplies its UI dispatcher and owns the returned component. */
fun createPlaybackComponent(dispatcher: CoroutineDispatcher): PlaybackComponent {
    val imageLoader = Landscapist.builder().build()
    return PlaybackComponent(dispatcher, imageLoader, { session ->
        MediaNotificationAdapter(session, MediaPlatform.desktop {
            linuxBusName = "org.mpris.MediaPlayer2.echo"
            linuxDesktopEntry = "dev.brahmkshatriya.echo"
        }, imageLoader)
    })
}
