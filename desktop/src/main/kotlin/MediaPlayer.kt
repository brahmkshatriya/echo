package dev.brahmkshatriya.echo

import io.github.selemba1000.JMTC
import io.github.selemba1000.JMTCButtonCallback
import io.github.selemba1000.JMTCCallbacks
import io.github.selemba1000.JMTCEnabledButtons
import io.github.selemba1000.JMTCMediaType
import io.github.selemba1000.JMTCMusicProperties
import io.github.selemba1000.JMTCPlayingState
import io.github.selemba1000.JMTCSettings
import io.github.selemba1000.JMTCTimelineProperties

fun mediaPlayer() {
    val jmtc = JMTC.getInstance(
        JMTCSettings("Echo", "echo-desktop-file")
    )
    val callbacks = JMTCCallbacks()
    callbacks.onPlay = JMTCButtonCallback {
        println("bruh playing")
        jmtc.playingState = JMTCPlayingState.PLAYING
    }
    callbacks.onPause = JMTCButtonCallback {
        println("paused")
        jmtc.setPlayingState(JMTCPlayingState.PAUSED)
    }
    jmtc.setEnabledButtons(
        JMTCEnabledButtons(
            true,
            true,
            true,
            true,
            true
        )
    )
    jmtc.setCallbacks(callbacks)
    jmtc.setEnabled(true)
    jmtc.setMediaType(JMTCMediaType.Music)
    jmtc.setPlayingState(JMTCPlayingState.PAUSED)
    jmtc.setMediaProperties(
        JMTCMusicProperties(
            "TestTitle",
            "TestArtist",
            "test",
            "tset",
            arrayOf<String?>(),
            0,
            1,
            null
        )
    )
    jmtc.setTimelineProperties(
        JMTCTimelineProperties(
            0L,
            100000L,
            0L,
            100000L
        )
    )
    jmtc.setPlayingState(JMTCPlayingState.PLAYING)
    jmtc.updateDisplay()
    jmtc.setPosition(6000L)
}