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
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionResult.RESULT_SUCCESS
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.listeners.Radio
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.toExceptionDetails
import dev.brahmkshatriya.echo.utils.future
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.searchNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@UnstableApi
class PlayerCallback(
    private val context: Context,
    private val settings: SharedPreferences,
    private val scope: CoroutineScope,
    private val extensionList: StateFlow<List<MusicExtension>?>,
    private val extensionFlow: StateFlow<MusicExtension?>,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    private val radioFlow: MutableStateFlow<Radio.State>
) : MediaLibrarySession.Callback {

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

    private fun setRepeat(player: Player, repeat: Int) = run {
        player.repeatMode = repeat
        Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    @OptIn(UnstableApi::class)
    private fun radio(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionError.ERROR_UNKNOWN)
        val clientId = args.getString("clientId") ?: return@future error
        val item = args.getSerialized<EchoMediaItem>("item") ?: return@future error
        radioFlow.value = Radio.State.Loading
        val loaded = Radio.start(
            context, messageFlow, throwableFlow, extensionList, clientId, item, null, 0
        )
        radioFlow.value = loaded ?: Radio.State.Empty
        if (loaded == null) return@future error
        val mediaItem = MediaItemUtils.build(
            settings, loaded.tracks[0], loaded.clientId, loaded.radio.toMediaItem()
        )
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        SessionResult(RESULT_SUCCESS)
    }

    override fun onSetRating(
        session: MediaSession, controller: MediaSession.ControllerInfo, rating: Rating
    ): ListenableFuture<SessionResult> {
        return if (rating !is ThumbRating) super.onSetRating(session, controller, rating)
        else scope.future {
            val errorIO = SessionResult(SessionError.ERROR_IO)
            val item = session.player.currentMediaItem ?: return@future errorIO
            extensionList.first { it != null }
            val extension = extensionList.getExtension(item.clientId) ?: return@future errorIO
            val client = extension.instance.value.getOrNull() ?: return@future errorIO
            if (client !is TrackLikeClient) return@future errorIO
            val track = item.track
            withContext(Dispatchers.IO) {
                runCatching {
                    client.likeTrack(track, rating.isThumbsUp)
                }
            }.getOrElse {
                return@future SessionResult(
                    SessionError.ERROR_UNKNOWN,
                    Bundle().apply { putSerialized("error", it.toExceptionDetails(context)) }
                )
            }
            val liked = rating.isThumbsUp
            val newItem = item.run {
                buildUpon().setMediaMetadata(
                    mediaMetadata.buildUpon().setUserRating(ThumbRating(liked)).build()
                )
            }.build()
            session.player.replaceMediaItem(session.player.currentMediaItemIndex, newItem)
            SessionResult(RESULT_SUCCESS, bundleOf("liked" to liked))
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val settable = SettableFuture.create<MediaItemsWithStartPosition>()
        val resumptionPlaylist = ResumptionUtils.recoverPlaylist(context)
        settable.set(resumptionPlaylist)
        return settable
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaItemsWithStartPosition> {
        radioFlow.value = Radio.State.Empty
        return super.onSetMediaItems(
            mediaSession, controller, mediaItems, startIndex, startPositionMs
        )
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
        val client =
            extension?.instance?.value?.getOrNull() ?: return default { noClient().message }
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
                    is Shelf.Lists.Items -> {
                        val items = it.list.mapNotNull { item ->
                            if (item is EchoMediaItem.TrackItem) item.track
                            else null
                        }
                        items
                    }

                    is Shelf.Lists.Tracks -> it.list

                    is Shelf.Item -> {
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
}