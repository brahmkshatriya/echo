@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kaudiooutput.bindAndroidDevice
import dev.brahmkshatriya.kmplayer.KMPlayer
import dev.brahmkshatriya.kmplayer.exoPlayer

actual fun bindAudioOutput(outputs: AudioOutputs, player: KMPlayer): AutoCloseable =
    outputs.bindAndroidDevice(requireNotNull(player.exoPlayer)::setPreferredAudioDevice)
