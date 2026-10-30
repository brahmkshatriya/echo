package dev.brahmkshatriya.echo.app.playback

import com.skydoves.landscapist.core.Landscapist
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kaudiooutput.desktop
import dev.brahmkshatriya.kmedianotification.MediaPlatform
import dev.brahmkshatriya.kmedianotification.desktop
import kotlinx.coroutines.CoroutineDispatcher

/** The desktop application supplies its UI dispatcher and owns the returned component. */
fun createPlaybackComponent(dispatcher: CoroutineDispatcher): PlaybackComponent {
    val imageLoader = Landscapist.builder().build()
    return PlaybackComponent(
        dispatcher,
        imageLoader,
        createSystemMediaSession = { session ->
            MediaNotificationAdapter(
                session,
                MediaPlatform.desktop {
                    linuxBusName = "org.mpris.MediaPlayer2.echo"
                    linuxDesktopEntry = "dev.brahmkshatriya.echo"
                },
                imageLoader
            )
        },
        createAudioOutputs = { AudioOutputs.desktop(it) }
    )
}
