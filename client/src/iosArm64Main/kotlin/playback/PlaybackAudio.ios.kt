@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.brahmkshatriya.echo.app.playback

import dev.brahmkshatriya.kmedianotification.MediaPlatform
import dev.brahmkshatriya.kmedianotification.apple
import platform.AVFAudio.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import kotlinx.coroutines.launch
import com.skydoves.landscapist.core.Landscapist

private fun activateIosPlaybackAudio() {
    val audio = AVAudioSession.sharedInstance()
    check(audio.setCategory(AVAudioSessionCategoryPlayback, error = null)) { "Could not set the playback audio category" }
    check(audio.setActive(true, error = null)) { "Could not activate the playback audio session" }
}

private fun observeIosPlaybackAudio(session: PlayerSession): AutoCloseable {
    val center = NSNotificationCenter.defaultCenter
    val interrupted = center.addObserverForName(AVAudioSessionInterruptionNotification,
        AVAudioSession.sharedInstance(), NSOperationQueue.mainQueue) { notification ->
        val type = (notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedIntegerValue
        if (type == AVAudioSessionInterruptionTypeBegan) session.scope.launch { session.pause() }
    }
    val route = center.addObserverForName(AVAudioSessionRouteChangeNotification,
        AVAudioSession.sharedInstance(), NSOperationQueue.mainQueue) { notification ->
        val reason = (notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)?.unsignedIntegerValue
        if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) session.scope.launch { session.pause() }
    }
    return AutoCloseable {
        center.removeObserver(interrupted)
        center.removeObserver(route)
    }
}

fun createIosSystemMediaSession(session: PlayerSession, imageLoader: Landscapist): SystemMediaSession {
    val adapter = MediaNotificationAdapter(session, MediaPlatform.apple(), imageLoader)
    val audioEvents = observeIosPlaybackAudio(session)
    return object : SystemMediaSession {
        override val notification get() = adapter.notification
        override fun activate() {
            activateIosPlaybackAudio()
            adapter.activate()
        }
        override fun update(state: SystemMediaState) = adapter.update(state)
        override fun close() {
            audioEvents.close()
            adapter.close()
        }
    }
}
