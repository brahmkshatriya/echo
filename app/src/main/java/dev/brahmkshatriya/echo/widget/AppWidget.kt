package dev.brahmkshatriya.echo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLiked
import dev.brahmkshatriya.echo.playback.PlayerCommands.likeCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.repeatCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.repeatOffCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.repeatOneCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.resumeCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.unlikeCommand
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getController
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getPendingIntent
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverPlaylist
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppWidget : AppWidgetProvider(), KoinComponent {

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        updateWidgets(context)
    }

    override fun onEnabled(context: Context) {
        updateWidgets(context)
    }

    override fun onDisabled(context: Context) {
        controllerCallback?.invoke()
        controller = null
        controllerCallback = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        ensureController(context ?: return)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> controller?.run {
                prepare()
                playWhenReady = !playWhenReady
            }

            ACTION_PREVIOUS -> controller?.seekToPrevious()
            ACTION_NEXT -> controller?.seekToNext()
            ACTION_LIKE -> controller?.sendCustomCommand(likeCommand, Bundle.EMPTY)
            ACTION_UNLIKE -> controller?.sendCustomCommand(unlikeCommand, Bundle.EMPTY)
            ACTION_REPEAT -> controller?.sendCustomCommand(repeatCommand, Bundle.EMPTY)
            ACTION_REPEAT_OFF -> controller?.sendCustomCommand(repeatOffCommand, Bundle.EMPTY)
            ACTION_REPEAT_ONE -> controller?.sendCustomCommand(repeatOneCommand, Bundle.EMPTY)
            ACTION_RESUME -> controller?.run {
                sendCustomCommand(resumeCommand, bundleOf("cleared" to false))
                playWhenReady = true
            }

            else -> super.onReceive(context, intent)
        }
    }

    private val app by inject<App>()

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        context ?: return
        appWidgetManager ?: return
        ensureController(context)
        updateAppWidget(controller, context, appWidgetId, appWidgetManager)
    }

    private fun ensureController(context: Context) {
        if (controllerCallback != null) return
        val listener = WidgetPlayerListener {
            image = it
            updateWidgets(context)
        }

        val callback = getController(app.context) {
            controller = it
            listener.controller = it
            it.addListener(listener)
            updateWidgets(context)
        }

        controllerCallback = {
            listener.removed()
            callback()
        }
    }

    private fun updateWidgets(context: Context) {
        ensureController(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, AppWidget::class.java)
        )
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(controller, context, appWidgetId, appWidgetManager)
        }
    }

    companion object {
        private var controller: MediaController? = null
        private var image: Bitmap? = null
        private var controllerCallback: (() -> Unit)? = null

        private const val ACTION_PLAY_PAUSE = "dev.brahmkshatriya.echo.widget.PLAY_PAUSE"
        private const val ACTION_PREVIOUS = "dev.brahmkshatriya.echo.widget.PREVIOUS"
        private const val ACTION_NEXT = "dev.brahmkshatriya.echo.widget.NEXT"
        private const val ACTION_LIKE = "dev.brahmkshatriya.echo.widget.LIKE"
        private const val ACTION_UNLIKE = "dev.brahmkshatriya.echo.widget.UNLIKE"
        private const val ACTION_REPEAT = "dev.brahmkshatriya.echo.widget.REPEAT"
        private const val ACTION_REPEAT_OFF = "dev.brahmkshatriya.echo.widget.REPEAT_OFF"
        private const val ACTION_REPEAT_ONE = "dev.brahmkshatriya.echo.widget.REPEAT_ONE"
        private const val ACTION_RESUME = "dev.brahmkshatriya.echo.widget.RESUME"

        private fun updateAppWidget(
            controller: MediaController?,
            context: Context,
            appWidgetId: Int,
            appWidgetManager: AppWidgetManager
        ) {
            val packageName = context.packageName
            val large = RemoteViews(packageName, R.layout.app_widget_large)
            val medium = RemoteViews(packageName, R.layout.app_widget_medium)
            val small = RemoteViews(packageName, R.layout.app_widget_small)

            val view = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                updateView(controller, context, small)
                small
            } else {
                val widgetLayout = mapOf(
                    SizeF(0f, 100f) to small,
                    SizeF(200f, 100f) to medium,
                    SizeF(300f, 100f) to large,
                )
                widgetLayout.forEach { (_, u) ->
                    updateView(controller, context, u)
                }
                RemoteViews(widgetLayout)
            }

            appWidgetManager.updateAppWidget(appWidgetId, view)
        }

        private fun Context.createIntent(action: String) = PendingIntent.getBroadcast(
            this, 0, Intent(this, AppWidget::class.java).apply {
                this.action = action
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        private fun updateView(
            controller: MediaController?,
            context: Context,
            views: RemoteViews,
        ) {
            val current = controller?.currentMediaItem
            val item = current ?: context.run {
                val (list, index) = runBlocking { recoverPlaylist(listOf(), false) }
                list.getOrNull(index)
            }
            val title = item?.mediaMetadata?.title
            val artist = item?.mediaMetadata?.artist
            views.setTextViewText(R.id.trackTitle, title ?: context.getString(R.string.so_empty))
            views.setTextViewText(
                R.id.trackArtist,
                artist ?: context.getString(R.string.unknown).takeIf { title != null })
            if (image == null) views.setImageViewResource(R.id.trackCover, R.drawable.art_music)
            else views.setImageViewBitmap(R.id.trackCover, image)

            views.setOnClickPendingIntent(android.R.id.background, getPendingIntent(context))

            val isPlaying = if (current != null) controller.playWhenReady else false
            views.setOnClickPendingIntent(
                R.id.playPauseButton, context.createIntent(
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

//            views.setViewVisibility(
//                R.id.playProgress, if (controller?.isLoading == true) VISIBLE else GONE
//            )

            views.setOnClickPendingIntent(
                R.id.nextButton, context.createIntent(ACTION_NEXT)
            )
            views.setFloat(
                R.id.nextButton,
                "setAlpha",
                if (controller?.hasNextMediaItem() == true) 1f else 0.5f
            )
            views.setOnClickPendingIntent(
                R.id.previousButton, context.createIntent(ACTION_PREVIOUS)
            )
            views.setFloat(
                R.id.previousButton,
                "setAlpha",
                if ((controller?.currentMediaItemIndex ?: -1) >= 0) 1f else 0.5f
            )

            val isLiked = item?.isLiked ?: false
            views.setOnClickPendingIntent(
                R.id.likeButton, context.createIntent(if (isLiked) ACTION_UNLIKE else ACTION_LIKE)
            )
            views.setImageViewResource(
                R.id.likeButton,
                if (isLiked) R.drawable.ic_favorite_filled_20dp else R.drawable.ic_favorite_20dp
            )

            val repeatMode = controller?.repeatMode ?: 0
            views.setOnClickPendingIntent(
                R.id.repeatButton, context.createIntent(
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
