package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kmplayer.KMPlayer

/** Echo only supplies a native player binding; discovery and routing live in KAudioOutput. */
expect fun bindAudioOutput(outputs: AudioOutputs, player: KMPlayer): AutoCloseable
