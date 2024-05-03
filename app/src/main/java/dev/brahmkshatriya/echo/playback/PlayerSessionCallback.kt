package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.ListUpdateCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.searchNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch


class PlayerSessionCallback(
    private val context: Context,
    private val scope: CoroutineScope,
    private val global: Queue,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    extensionFlow: Flow<ExtensionClient?>,
) : MediaLibraryService.MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        return MediaSession.ConnectionResult.accept(
            connectionResult.availableSessionCommands.buildUpon()
                .add(likeCommand).add(unlikeCommand)
                .add(repeatCommand).add(repeatOffCommand).add(repeatOneCommand)
                .build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        val player = session.player
        val mediaId = player.currentMediaItem?.mediaId
        return when (customCommand) {
            likeCommand -> rateMediaItem(ThumbRating(true), mediaId)
            unlikeCommand -> rateMediaItem(ThumbRating(false), mediaId)
            repeatOffCommand -> setRepeat(player, Player.REPEAT_MODE_OFF)
            repeatOneCommand -> setRepeat(player, Player.REPEAT_MODE_ONE)
            repeatCommand -> setRepeat(player, Player.REPEAT_MODE_ALL)
            else -> super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun setRepeat(player: Player, repeat: Int) = run {
        player.repeatMode = repeat
        Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    private fun rateMediaItem(
        rating: ThumbRating,
        mediaId: String? = null,
    ) = scope.future {
        val errorIO = SessionResult(SessionResult.RESULT_ERROR_IO)
        val streamableTrack = global.getTrack(mediaId) ?: return@future errorIO
        val client = extensionListFlow.getClient(streamableTrack.clientId) ?: return@future errorIO
        if (client !is LibraryClient) return@future errorIO
        val track = streamableTrack.current
        val liked = runCatching {
            println("${rating.isThumbsUp}")
            client.likeTrack(track, rating.isThumbsUp)
        }.getOrElse {
            streamableTrack.onLiked.emit(track.liked)
            return@future SessionResult(
                SessionResult.RESULT_ERROR_UNKNOWN,
                Bundle().apply { putSerializable("error", it) }
            )
        }
        streamableTrack.liked = liked
        streamableTrack.onLiked.emit(liked)
        SessionResult(RESULT_SUCCESS)
    }

    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        rating: Rating
    ): ListenableFuture<SessionResult> {
        return if (rating !is ThumbRating) super.onSetRating(session, controller, rating)
        else {
            val mediaId = session.player.currentMediaItem?.mediaId
                ?: return super.onSetRating(session, controller, rating)
            rateMediaItem(rating, mediaId)
        }
    }

    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaId: String,
        rating: Rating
    ): ListenableFuture<SessionResult> {
        return if (rating !is ThumbRating) super.onSetRating(session, controller, rating)
        else rateMediaItem(rating, mediaId)
    }


    // Google Assistant Stuff

    init {
        scope.collect(extensionFlow) { extension = it }
    }

    private var extension: ExtensionClient? = null

    private val updateCallback = object : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) = Unit
        override fun onMoved(fromPosition: Int, toPosition: Int) = Unit
        override fun onInserted(position: Int, count: Int) = Unit
        override fun onRemoved(position: Int, count: Int) = Unit
    }

    private suspend fun <T : Any> Flow<PagingData<T>>.getItems(
        differ: AsyncPagingDataDiffer<T>
    ) = coroutineScope {
        val job = launch { collect { differ.submitData(it) } }
        val refresh = differ.loadStateFlow
            .first { it.refresh !is LoadState.Loading }
            .refresh
        job.cancel()
        if (refresh is LoadState.Error) throw refresh.error
        differ.snapshot().items
    }

    private val handler = Handler(Looper.getMainLooper())
    private fun toast(message: String) {
        handler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {

        //Look at the first item's search query, if it's null, return the super method
        val query = mediaItems.firstOrNull()?.requestMetadata?.searchQuery
            ?: return super.onAddMediaItems(mediaSession, controller, mediaItems)

        fun default(reason: Context.() -> String): ListenableFuture<MutableList<MediaItem>> {
            toast(reason.invoke(context))
            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }

        val client = extension
            ?: return default { noClient().message }
        if (client !is SearchClient)
            return default { searchNotSupported(client.metadata.id).message }
        if (client !is TrackClient)
            return default { trackNotSupported(client.metadata.id).message }
        val flow = client.search(query, null)
            .catch { default { it.message ?: "Unknown Error" } }
        val differ = AsyncPagingDataDiffer(MediaContainerAdapter.DiffCallback, updateCallback)

        return scope.future {
            val itemsContainers = try {
                flow.getItems(differ)
            } catch (e: Throwable) {
                default { e.message ?: "Unknown Error" }
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
                        val item = it.media as? EchoMediaItem.TrackItem
                            ?: return@mapNotNull null
                        listOf(item.track)
                    }

                    else -> null
                }
            }.flatten()
            if (tracks.isEmpty()) default { getString(R.string.could_not_find_anything, query) }
            val items = global.addTracks(client.metadata.id, tracks).second
            items.toMutableList()
        }
    }

    @UnstableApi
    override fun onSetMediaItems(
        mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long
    ) = scope.future {
        global.clearQueue()
        super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            .get()
    }

}