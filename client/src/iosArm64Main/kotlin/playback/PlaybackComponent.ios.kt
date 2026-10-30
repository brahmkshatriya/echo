package dev.brahmkshatriya.echo.app.playback

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.skydoves.landscapist.core.Landscapist
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kaudiooutput.ios
import dev.brahmkshatriya.kmplayer.KMPlayer

fun createPlaybackComponent(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
): PlaybackComponent {
    val imageLoader = Landscapist.builder().build()
    return PlaybackComponent(dispatcher, imageLoader, { session -> createIosSystemMediaSession(session, imageLoader) },
        createAudioOutputs = { AudioOutputs.ios(it) })
}

actual fun bindAudioOutput(outputs: AudioOutputs, player: KMPlayer): AutoCloseable = AutoCloseable {}
