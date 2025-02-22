package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.ThumbRating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionResult.RESULT_SUCCESS
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.Extensions
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverPlaylist
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverRepeat
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverShuffle
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.CoroutineUtils.future
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask


@OptIn(UnstableApi::class)
class PlayerCallback(
    override val context: Context,
    override val scope: CoroutineScope,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val extensions: Extensions,
    private val radioFlow: MutableStateFlow<PlayerState.Radio>,
) : AndroidAutoCallback(context, scope, extensions.music) {

    override fun onConnect(
        session: MediaSession, controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands = with(PlayerCommands) {
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(likeCommand).add(unlikeCommand).add(repeatCommand).add(repeatOffCommand)
                .add(repeatOneCommand).add(radioCommand).add(sleepTimer).build()
        }
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands).build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> = with(PlayerCommands) {
        val player = session.player
        when (customCommand) {
            likeCommand -> onSetRating(session, controller, ThumbRating(true))
            unlikeCommand -> onSetRating(session, controller, ThumbRating())
            repeatOffCommand -> setRepeat(player, Player.REPEAT_MODE_OFF)
            repeatOneCommand -> setRepeat(player, Player.REPEAT_MODE_ONE)
            repeatCommand -> setRepeat(player, Player.REPEAT_MODE_ALL)
            radioCommand -> radio(player, args)
            sleepTimer -> onSleepTimer(player, args.getLong("ms"))
            else -> super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private inner class SleepTimerTask(private val player: Player) : TimerTask() {
        override fun run() {
            runBlocking(Dispatchers.Main) { player.pause() }
        }
    }

    private var timer = Timer()
    private fun onSleepTimer(player: Player, ms: Long): ListenableFuture<SessionResult> {
        timer.cancel()
        val time = when (ms) {
            0L -> return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
            Long.MAX_VALUE -> player.run { duration - currentPosition }
            else -> ms
        }
        timer = Timer()
        timer.schedule(SleepTimerTask(player), time)
        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    private fun setRepeat(player: Player, repeat: Int) = run {
        player.repeatMode = repeat
        Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    val settings = context.getSettings()

    @OptIn(UnstableApi::class)
    private fun radio(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionError.ERROR_UNKNOWN)
        val clientId = args.getString("extId") ?: return@future error
        val item = args.getSerialized<EchoMediaItem>("item") ?: return@future error
        radioFlow.value = PlayerState.Radio.Loading
        val loaded = PlayerRadio.start(
            throwableFlow, extensionList, clientId, item, null
        )
        if (loaded == null) return@future error
        PlayerRadio.play(player, settings, radioFlow, loaded)
        SessionResult(RESULT_SUCCESS)
    }

    override fun onSetRating(
        session: MediaSession, controller: MediaSession.ControllerInfo, rating: Rating
    ): ListenableFuture<SessionResult> {
        return if (rating !is ThumbRating) super.onSetRating(session, controller, rating)
        else scope.future {
            val item = withContext(Dispatchers.Main) { session.player.currentMediaItem }
                ?: return@future SessionResult(SessionError.ERROR_IO)
            val track = item.track
            runCatching {
                val extension = extensions.music.getExtensionOrThrow(item.extensionId)
                extension.get<TrackLikeClient, Unit> {
                    likeTrack(track, rating.isThumbsUp)
                }
            }.getOrElse {
                return@future SessionResult(
                    SessionError.ERROR_UNKNOWN,
                    bundleOf(
                        "error" to it,
                        "trace" to it.stackTraceToString()
                    )
                )
            }
            val liked = rating.isThumbsUp
            val newItem = item.run {
                buildUpon().setMediaMetadata(
                    mediaMetadata.buildUpon().setUserRating(ThumbRating(liked)).build()
                )
            }.build()
            withContext(Dispatchers.Main) {
                session.player.replaceMediaItem(session.player.currentMediaItemIndex, newItem)
            }
            SessionResult(RESULT_SUCCESS, bundleOf("liked" to liked))
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        mediaSession.player.shuffleModeEnabled = context.recoverShuffle() ?: false
        mediaSession.player.repeatMode = context.recoverRepeat() ?: Player.REPEAT_MODE_OFF
        val (items, index, pos) = context.recoverPlaylist()
        return Futures.immediateFuture(MediaItemsWithStartPosition(items, index, pos))
    }
}