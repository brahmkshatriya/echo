package dev.brahmkshatriya.echo.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import dev.brahmkshatriya.kmedianotification.AndroidMediaConfig
import dev.brahmkshatriya.kmedianotification.MediaPlatform
import dev.brahmkshatriya.kmedianotification.android
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.builder
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import dev.brahmkshatriya.kaudiooutput.android

/** Implemented by the Android Application, which outlives Activities and services. */
interface PlaybackComponentOwner {
    val playbackComponent: PlaybackComponent
}

fun createPlaybackComponent(
    context: Context,
    @DrawableRes notificationIcon: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    configureMedia: AndroidMediaConfig.() -> Unit = {},
): PlaybackComponent {
    val applicationContext = context.applicationContext
    val imageLoader = Landscapist.builder(applicationContext).build()
    return PlaybackComponent(dispatcher, imageLoader, { session ->
        MediaNotificationAdapter(session, MediaPlatform.android(applicationContext) {
            configureMedia()
            serviceClass = EchoPlaybackService::class.java
            notification {
                channelId = "echo.playback"
                smallIcon = notificationIcon
            }
            sessionActivity = applicationContext.packageManager
                .getLaunchIntentForPackage(applicationContext.packageName)?.let {
                    PendingIntent.getActivity(applicationContext, 0,
                        it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                }
        }, imageLoader)
    }, createAudioOutputs = { AudioOutputs.android(applicationContext, it) })
}
