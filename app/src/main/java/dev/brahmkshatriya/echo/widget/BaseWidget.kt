package dev.brahmkshatriya.echo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.playback.MediaItemUtils.state
import dev.brahmkshatriya.echo.playback.PlayerCommands.likeCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.repeatCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.repeatOffCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.repeatOneCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.resumeCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.unlikeCommand
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getPendingIntent
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverIndex
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverTracks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseWidget : AppWidgetProvider(), KoinComponent {
    abstract val clazz: Class<*>
    private val key by lazy { clazz.name }

    private val app by inject<App>()
    override fun onEnabled(context: Context) {
        println("Widget enabled $this")
        ControllerHelper.register(app, key) {
            updateWidgets(app.context)
        }
    }

    override fun onDisabled(context: Context) {
        println("Widget disabled $this")
        ControllerHelper.unregister(key)
    }

    override fun onUpdate(
        context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?,
    ) {
        println("Widget update ${appWidgetIds?.toList()} $this")
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        println("Widget received ${intent?.action} $this")
        ControllerHelper.register(app, key) {
            updateWidgets(app.context)
        }
        val controller = ControllerHelper.controller ?: return
        println("Controller is $controller")
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> controller.run {
                prepare()
                playWhenReady = !playWhenReady
            }

            ACTION_PREVIOUS -> controller.seekToPrevious()
            ACTION_NEXT -> controller.seekToNext()
            ACTION_LIKE -> controller.sendCustomCommand(likeCommand, Bundle.EMPTY)
            ACTION_UNLIKE -> controller.sendCustomCommand(unlikeCommand, Bundle.EMPTY)
            ACTION_REPEAT -> controller.sendCustomCommand(repeatCommand, Bundle.EMPTY)
            ACTION_REPEAT_OFF -> controller.sendCustomCommand(repeatOffCommand, Bundle.EMPTY)
            ACTION_REPEAT_ONE -> controller.sendCustomCommand(repeatOneCommand, Bundle.EMPTY)
            ACTION_RESUME -> controller.run {
                sendCustomCommand(resumeCommand, bundleOf("cleared" to false))
                playWhenReady = true
            }

            else -> super.onReceive(context, intent)
        }
    }


    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        context ?: return
        appWidgetManager ?: return
        ControllerHelper.register(app, key) {
            updateWidgets(app.context)
        }
        val controller = ControllerHelper.controller
        val image = ControllerHelper.image
        val views = updatedViews(controller, image, context, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateWidgets(context: Context) {
        val controller = ControllerHelper.controller
        val image = ControllerHelper.image
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, clazz)
        )
        appWidgetIds.forEach { appWidgetId ->
            val views = updatedViews(controller, image, context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    abstract fun updatedViews(
        controller: MediaController?, image: Bitmap?, context: Context, appWidgetId: Int,
    ): RemoteViews

    companion object {
        const val ACTION_PLAY_PAUSE = "dev.brahmkshatriya.echo.widget.PLAY_PAUSE"
        const val ACTION_PREVIOUS = "dev.brahmkshatriya.echo.widget.PREVIOUS"
        const val ACTION_NEXT = "dev.brahmkshatriya.echo.widget.NEXT"
        const val ACTION_LIKE = "dev.brahmkshatriya.echo.widget.LIKE"
        const val ACTION_UNLIKE = "dev.brahmkshatriya.echo.widget.UNLIKE"
        const val ACTION_REPEAT = "dev.brahmkshatriya.echo.widget.REPEAT"
        const val ACTION_REPEAT_OFF = "dev.brahmkshatriya.echo.widget.REPEAT_OFF"
        const val ACTION_REPEAT_ONE = "dev.brahmkshatriya.echo.widget.REPEAT_ONE"
        const val ACTION_RESUME = "dev.brahmkshatriya.echo.widget.RESUME"

        fun Context.createIntent(clazz: Class<*>, action: String) = PendingIntent.getBroadcast(
            this, 0, Intent(this, clazz).apply {
                this.action = action
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )!!

        fun updateView(
            clazz: Class<*>,
            controller: MediaController?,
            image: Bitmap?,
            context: Context,
            views: RemoteViews,
        ) {
            val current = controller?.currentMediaItem
            val item = current?.state ?: context.run {
                val list = recoverTracks().orEmpty()
                val index = recoverIndex() ?: 0
                list.getOrNull(index)?.first
            }
            val title = item?.item?.title
            val artist = item?.item?.artists?.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { it.name }
            views.setTextViewText(R.id.trackTitle, title ?: context.getString(R.string.so_empty))
            views.setTextViewText(
                R.id.trackArtist,
                artist ?: context.getString(R.string.unknown).takeIf { title != null }
            )
            val image = image?.run { copy(config ?: Bitmap.Config.ARGB_8888, false) }
            if (image == null) views.setImageViewResource(R.id.trackCover, R.drawable.art_music)
            else views.setImageViewBitmap(R.id.trackCover, image)

            views.setOnClickPendingIntent(android.R.id.background, getPendingIntent(context))

            val isPlaying = if (current != null) controller.playWhenReady else false
            views.setOnClickPendingIntent(
                R.id.playPauseButton, context.createIntent(
                    clazz,
                    if (current != null) {
                        if (isPlaying) ACTION_PLAY_PAUSE else ACTION_PLAY_PAUSE
                    } else ACTION_RESUME
                )
            )
            views.setFloat(R.id.playPauseButton, "setAlpha", if (item != null) 1f else 0.5f)

            views.setImageViewResource(
                R.id.playPauseButton,
                if (isPlaying) R.drawable.ic_pause_48dp else R.drawable.ic_play_48dp
            )

            views.setOnClickPendingIntent(
                R.id.nextButton, context.createIntent(clazz, ACTION_NEXT)
            )
            views.setFloat(
                R.id.nextButton,
                "setAlpha",
                if (controller?.hasNextMediaItem() == true) 1f else 0.5f
            )
            views.setOnClickPendingIntent(
                R.id.previousButton, context.createIntent(clazz, ACTION_PREVIOUS)
            )
            views.setFloat(
                R.id.previousButton,
                "setAlpha",
                if ((controller?.currentMediaItemIndex ?: -1) >= 0) 1f else 0.5f
            )

            val isLiked = (item as? MediaState.Loaded)?.isLiked ?: false
            views.setOnClickPendingIntent(
                R.id.likeButton,
                context.createIntent(clazz, if (isLiked) ACTION_UNLIKE else ACTION_LIKE)
            )
            views.setImageViewResource(
                R.id.likeButton,
                if (isLiked) R.drawable.ic_favorite_filled_20dp else R.drawable.ic_favorite_20dp
            )

            val repeatMode = controller?.repeatMode ?: 0
            views.setOnClickPendingIntent(
                R.id.repeatButton, context.createIntent(
                    clazz,
                    when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> ACTION_REPEAT
                        Player.REPEAT_MODE_ALL -> ACTION_REPEAT_ONE
                        else -> ACTION_REPEAT_OFF
                    }
                )
            )

            views.setImageViewResource(
                R.id.repeatButton, when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> R.drawable.ic_repeat_20dp
                    Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_20dp
                    else -> R.drawable.ic_repeat_on_20dp
                }
            )
        }
    }
}