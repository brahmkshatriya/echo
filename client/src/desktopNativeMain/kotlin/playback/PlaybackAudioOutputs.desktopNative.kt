package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kmplayer.KMBackendKind
import dev.brahmkshatriya.kmplayer.KMPlayer
import dev.brahmkshatriya.kmplayer.getOrThrow

// Linux uses process-level PipeWire routing and Windows uses the system mixer.
// AVFoundation needs a binding for each player so the Mac selection is per-app.
actual fun bindAudioOutput(outputs: AudioOutputs, player: KMPlayer): AutoCloseable =
    if (player.capabilities.backend != KMBackendKind.AvFoundation) AutoCloseable {}
    else outputs.bind { device ->
        player.setAudioOutputDevice(device?.id).getOrThrow()
    }
