package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.ThumbRating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionResult.RESULT_SUCCESS
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.utils.getParcel
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.searchNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext

class PlayerSessionCallback(
    private val context: Context,
    private val settings: SharedPreferences,
    private val scope: CoroutineScope,
    private val extensionList: StateFlow<List<MusicExtension>?>,
    private val extensionFlow: StateFlow<MusicExtension?>,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    private val radioFlow: MutableStateFlow<Radio.State>
) : MediaLibraryService.MediaLibrarySession.Callback {

    @OptIn(UnstableApi::class)
    override fun onConnect(
        session: MediaSession, controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands = with(PlayerCommands) {
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(likeCommand).add(unlikeCommand).add(repeatCommand).add(repeatOffCommand)
                .add(repeatOneCommand).add(radioCommand).build()
        }
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands).build()
    }

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> = with(PlayerCommands) {
        val player = session.player
        when (customCommand) {
            likeCommand -> onSetRating(session, controller, ThumbRating(true))
            unlikeCommand -> onSetRating(session, controller, ThumbRating(false))
            repeatOffCommand -> setRepeat(player, Player.REPEAT_MODE_OFF)
            repeatOneCommand -> setRepeat(player, Player.REPEAT_MODE_ONE)
            repeatCommand -> setRepeat(player, Player.REPEAT_MODE_ALL)
            radioCommand -> radio(player, args)
            else -> super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun radio(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionResult.RESULT_ERROR_UNKNOWN)
        val clientId = args.getString("clientId") ?: return@future error
        val item = args.getParcel<EchoMediaItem>("item") ?: return@future error
        val loaded = Radio.start(
            context, messageFlow, throwableFlow, radioFlow, extensionList, clientId, item, 0
        ) ?: return@future error
        val mediaItem = MediaItemUtils.build(
            settings, loaded.tracks[0], loaded.clientId, loaded.playlist.toMediaItem()
        )
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        SessionResult(RESULT_SUCCESS)
    }

    private fun setRepeat(player: Player, repeat: Int) = run {
        player.repeatMode = repeat
        Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    override fun onSetRating(
        session: MediaSession, controller: MediaSession.ControllerInfo, rating: Rating
    ): ListenableFuture<SessionResult> {
        return if (rating !is ThumbRating) super.onSetRating(session, controller, rating)
        else scope.future {
            val errorIO = SessionResult(SessionResult.RESULT_ERROR_IO)
            val item = session.player.currentMediaItem ?: return@future errorIO
            val client = extensionList.getExtension(item.clientId)?.client ?: return@future errorIO
            if (client !is LibraryClient) return@future errorIO
            val track = item.track
            val liked = withContext(Dispatchers.IO) {
                runCatching { client.likeTrack(track, rating.isThumbsUp) }
            }.getOrElse {
                return@future SessionResult(
                    SessionResult.RESULT_ERROR_UNKNOWN, bundleOf("error" to it)
                )
            }
            val newItem = item.run {
                buildUpon().setMediaMetadata(
                    mediaMetadata.buildUpon().setUserRating(ThumbRating(liked)).build()
                )
            }.build()
            session.player.replaceMediaItem(session.player.currentMediaItemIndex, newItem)
            SessionResult(RESULT_SUCCESS, bundleOf("liked" to liked))
        }
    }


    @UnstableApi
    override fun onPlaybackResumption(
        mediaSession: MediaSession, controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        return@future ResumptionUtils.recoverPlaylist(context)
    }

    // Google Assistant Stuff

    private val handler = Handler(Looper.getMainLooper())
    private fun toast(message: String) {
        handler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {

        //Look at the first item's search query, if it's null, return the super method
        val query =
            mediaItems.firstOrNull()?.requestMetadata?.searchQuery ?: return super.onAddMediaItems(
                mediaSession,
                controller,
                mediaItems
            )

        fun default(reason: Context.() -> String): ListenableFuture<MutableList<MediaItem>> {
            toast(reason.invoke(context))
            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }

        val extension = extensionFlow.value
        val client = extension?.client ?: return default { noClient().message }
        val id = extension.metadata.id
        if (client !is SearchClient) return default { searchNotSupported(id).message }
        if (client !is TrackClient) return default { trackNotSupported(id).message }
        return scope.future {
            val itemsContainers = runCatching {
                client.searchFeed(query, null).loadFirst()
            }.getOrElse {
                default { it.message ?: "Unknown Error" }
                listOf()
            }

            val tracks = itemsContainers.mapNotNull {
                when (it) {
                    is MediaItemsContainer.Category -> {
                        val items = it.list.mapNotNull { item ->
                            if (item is EchoMediaItem.TrackItem) item.track
                            else null
                        }
                        items
                    }

                    is MediaItemsContainer.Item -> {
                        val item = it.media as? EchoMediaItem.TrackItem ?: return@mapNotNull null
                        listOf(item.track)
                    }

                    else -> null
                }
            }.flatten()
            if (tracks.isEmpty()) default { getString(R.string.could_not_find_anything, query) }
            tracks.map { MediaItemUtils.build(settings, it, id, null) }.toMutableList()
        }
    }

    @UnstableApi
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        radioFlow.value = Radio.State.Empty
        return super.onSetMediaItems(
            mediaSession, controller, mediaItems, startIndex, startPositionMs
        )
    }
}