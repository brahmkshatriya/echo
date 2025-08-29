package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.ThumbRating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionResult.RESULT_SUCCESS
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed.Companion.pagedDataOfFirst
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverPlaylist
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverRepeat
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverShuffle
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverTracks
import dev.brahmkshatriya.echo.playback.exceptions.PlayerException
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio
import dev.brahmkshatriya.echo.utils.CoroutineUtils.future
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(UnstableApi::class)
class PlayerCallback(
    override val app: App,
    override val scope: CoroutineScope,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val extensions: ExtensionLoader,
    private val radioFlow: MutableStateFlow<PlayerState.Radio>,
    override val downloadFlow: StateFlow<List<Downloader.Info>>,
) : AndroidAutoCallback(app, scope, extensions.music, downloadFlow) {

    override fun onConnect(
        session: MediaSession, controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val sessionCommands = with(PlayerCommands) {
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(likeCommand).add(unlikeCommand).add(repeatCommand).add(repeatOffCommand)
                .add(repeatOneCommand).add(radioCommand).add(sleepTimer)
                .add(playCommand).add(addToQueueCommand).add(addToNextCommand)
                .add(resumeCommand).add(imageCommand)
                .build()
        }
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands).build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> = with(PlayerCommands) {
        val player = session.player
        when (customCommand) {
            likeCommand -> onSetRating(session, controller, ThumbRating(true))
            unlikeCommand -> onSetRating(session, controller, ThumbRating())
            repeatOffCommand -> setRepeat(player, Player.REPEAT_MODE_OFF)
            repeatOneCommand -> setRepeat(player, Player.REPEAT_MODE_ONE)
            repeatCommand -> setRepeat(player, Player.REPEAT_MODE_ALL)
            playCommand -> playItem(player, args)
            addToQueueCommand -> addToQueue(player, args)
            addToNextCommand -> addToNext(player, args)
            radioCommand -> radio(player, args)
            sleepTimer -> onSleepTimer(player, args.getLong("ms"))
            resumeCommand -> resume(player, args.getBoolean("cleared", true))
            imageCommand -> getImage(player)
            else -> super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun getImage(player: Player) = scope.future {
        val item = player.with { currentMediaItem }
            ?: context.recoverPlaylist(app, downloadFlow.value, false).run { first.getOrNull(second) }
            ?: return@future SessionResult(SessionError.ERROR_UNKNOWN)
        val image = item.track.cover.loadDrawable(context)?.toScaledBitmap(720)
        SessionResult(RESULT_SUCCESS, Bundle().apply { putParcelable("image", image) })
    }

    private fun Drawable.toScaledBitmap(width: Int) = toBitmap().let { bmp ->
        val ratio = width.toFloat() / bmp.width
        val height = (bmp.height * ratio).toInt()
        bmp.scale(width, height)
    }

    private fun resume(player: Player, withClear: Boolean) = scope.future {
        withContext(Dispatchers.Main) {
            player.shuffleModeEnabled = context.recoverShuffle() == true
            player.repeatMode = context.recoverRepeat() ?: Player.REPEAT_MODE_OFF
        }
        val (items, index, pos) = context.recoverPlaylist(app,downloadFlow.value, withClear)
        withContext(Dispatchers.Main) {
            player.setMediaItems(items, index, pos)
            player.prepare()
        }
        SessionResult(RESULT_SUCCESS)
    }

    private var timerJob: Job? = null
    private fun onSleepTimer(player: Player, ms: Long): ListenableFuture<SessionResult> {
        timerJob?.cancel()
        val time = when (ms) {
            0L -> return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
            Long.MAX_VALUE -> player.run { duration - currentPosition }
            else -> ms
        }

        timerJob = scope.launch {
            delay(time)
            player.with { pause() }
        }
        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }

    private fun setRepeat(player: Player, repeat: Int) = run {
        player.repeatMode = repeat
        Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
    }


    @OptIn(UnstableApi::class)
    private fun radio(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionError.ERROR_UNKNOWN)
        val extId = args.getString("extId") ?: return@future error
        val item = args.getSerialized<EchoMediaItem>("item") ?: return@future error
        val itemLoaded = args.getBoolean("loaded", false)
        val extension = extensions.music.getExtension(extId) ?: return@future error
        val newItem = if (itemLoaded) item else loadItem(extension, item)
        radioFlow.value = PlayerState.Radio.Loading
        val loaded = PlayerRadio.start(
            throwableFlow, extension, newItem, null
        )
        if (loaded == null) return@future error
        player.with {
            clearMediaItems()
            shuffleModeEnabled = false
        }
        PlayerRadio.play(player, downloadFlow, app, radioFlow, loaded)
        player.with { play() }
        SessionResult(RESULT_SUCCESS)
    }

    private suspend fun loadItem(
        extension: Extension<*>, item: EchoMediaItem,
    ) = when (item) {
        is Track -> extension.getAs<TrackClient, EchoMediaItem> { loadTrack(item, false) }
        is Album -> extension.getAs<AlbumClient, EchoMediaItem> { loadAlbum(item) }
        is Playlist -> extension.getAs<PlaylistClient, EchoMediaItem> { loadPlaylist(item) }
        is Artist -> extension.getAs<ArtistClient, EchoMediaItem> { loadArtist(item) }
        is Radio -> throw IllegalStateException()
    }.getOrThrow()

    private suspend fun listTracks(
        extension: Extension<*>, item: EchoMediaItem, loaded: Boolean,
    ) = when (item) {
        is Album -> extension.getAs<AlbumClient, PagedData<Track>> {
            val album = if (!loaded) loadAlbum(item) else item
            loadTracks(album)?.pagedDataOfFirst()
                ?: PagedData.empty()
        }

        is Playlist -> extension.getAs<PlaylistClient, PagedData<Track>> {
            val playlist = if (!loaded) loadPlaylist(item) else item
            loadTracks(playlist).pagedDataOfFirst()
        }

        is Radio -> extension.getAs<RadioClient, PagedData<Track>> {
            val radio = if (!loaded) loadRadio(item) else item
            loadTracks(radio).pagedDataOfFirst()
        }

        is Artist -> extension.getAs<ArtistClient, PagedData<Track>> {
            val artist = if (!loaded) loadArtist(item) else item
            loadFeed(artist).pagedDataOfFirst().toTracks()
        }

        is Track -> Result.success(PagedData.Single { listOf(item) })
    }


    private fun playItem(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionError.ERROR_UNKNOWN)
        val extId = args.getString("extId") ?: return@future error
        val item = args.getSerialized<EchoMediaItem>("item") ?: return@future error
        val loaded = args.getBoolean("loaded", false)
        val shuffle = args.getBoolean("shuffle", false)
        val extension = extensions.music.getExtension(extId) ?: return@future error
        when (item) {
            is Track -> {
                val mediaItem = MediaItemUtils.build(
                    app, downloadFlow.value, MediaState.Unloaded(extId, item), null
                )
                player.with {
                    setMediaItem(mediaItem)
                    prepare()
                    seekTo(item.playedDuration ?: 0)
                    play()
                }
            }

            else -> {
                val tracks = listTracks(extension, item, loaded).getOrElse {
                    throwableFlow.emit(it)
                    return@future error
                }

                val result = if (shuffle) extension.get { tracks.loadAll() }
                else runCatching {
                    val (list, continuation) = extension.get { tracks.loadPage(null) }.getOrThrow()
                    if (continuation != null) scope.launch {
                        val all = extension.get { tracks.loadAll() }.getOrElse {
                            throwableFlow.emit(it)
                            return@launch
                        }.drop(list.size).map {
                            MediaItemUtils.build(
                                app, downloadFlow.value, MediaState.Unloaded(extId, it), item
                            )
                        }
                        player.with { addMediaItems(list.size, all) }
                    }
                    list
                }
                val list = result.getOrElse {
                    throwableFlow.emit(it)
                    return@future error
                }
                player.with {
                    setMediaItems(list.map {
                        MediaItemUtils.build(
                            app, downloadFlow.value, MediaState.Unloaded(extId, it), item
                        )
                    })
                    shuffleModeEnabled = shuffle
                    seekTo(0, list.firstOrNull()?.playedDuration ?: 0)
                    play()
                }
            }
        }
        SessionResult(RESULT_SUCCESS)
    }

    private suspend fun <T> Player.with(block: suspend Player.() -> T): T =
        withContext(Dispatchers.Main) { block() }

    private suspend fun <T : Any> PagedData<T>.load(
        pages: Int = 5,
    ) = runCatching {
        val list = mutableListOf<T>()
        var page = loadPage(null)
        list.addAll(page.data)
        var count = 0
        while (page.continuation != null && count < pages) {
            page = loadPage(page.continuation)
            list.addAll(page.data)
            count++
        }
        list
    }

    private fun addToQueue(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionError.ERROR_UNKNOWN)
        val extId = args.getString("extId") ?: return@future error
        val item = args.getSerialized<EchoMediaItem>("item") ?: return@future error
        val loaded = args.getBoolean("loaded", false)
        val extension = extensions.music.getExtension(extId) ?: return@future error
        val tracks = listTracks(extension, item, loaded).getOrElse {
            throwableFlow.emit(it)
            return@future error
        }.load().getOrElse {
            throwableFlow.emit(it)
            return@future error
        }
        if (tracks.isEmpty()) return@future error
        val mediaItems = tracks.map { track ->
            MediaItemUtils.build(
                app,
                downloadFlow.value,
                MediaState.Unloaded(extId, track),
                null
            )
        }
        player.with {
            addMediaItems(mediaItems)
            prepare()
        }
        SessionResult(RESULT_SUCCESS)
    }

    private var next = 0
    private var nextJob: Job? = null
    private fun addToNext(player: Player, args: Bundle) = scope.future {
        val error = SessionResult(SessionError.ERROR_UNKNOWN)
        val extId = args.getString("extId") ?: return@future error
        val item = args.getSerialized<EchoMediaItem>("item") ?: return@future error
        val loaded = args.getBoolean("loaded", false)
        val extension = extensions.music.getExtension(extId) ?: return@future error
        nextJob?.cancel()
        val tracks = listTracks(extension, item, loaded).getOrElse {
            throwableFlow.emit(it)
            return@future error
        }.load().getOrElse {
            throwableFlow.emit(it)
            return@future error
        }
        if (tracks.isEmpty()) return@future error
        val mediaItems = tracks.map { track ->
            MediaItemUtils.build(
                app,
                downloadFlow.value,
                MediaState.Unloaded(extId, track),
                null
            )
        }
        player.with {
            if (mediaItemCount == 0) playWhenReady = true
            addMediaItems(currentMediaItemIndex + 1 + next, mediaItems)
            prepare()
        }
        next += mediaItems.size
        nextJob = scope.launch {
            delay(5000)
            next = 0
        }
        SessionResult(RESULT_SUCCESS)
    }

    override fun onSetRating(
        session: MediaSession, controller: MediaSession.ControllerInfo, rating: Rating,
    ): ListenableFuture<SessionResult> {
        return if (rating !is ThumbRating) super.onSetRating(session, controller, rating)
        else scope.future {
            val item = session.player.with { currentMediaItem }
                ?: return@future SessionResult(SessionError.ERROR_UNKNOWN)
            val track = item.track
            runCatching {
                val extension = extensions.music.getExtensionOrThrow(item.extensionId)
                extension.getAs<LikeClient, Unit> {
                    likeItem(track, rating.isThumbsUp)
                }
            }.getOrElse {
                throwableFlow.emit(PlayerException(item, it))
                return@future SessionResult(SessionError.ERROR_UNKNOWN)
            }
            val liked = rating.isThumbsUp
            val newItem = item.run {
                buildUpon().setMediaMetadata(
                    mediaMetadata.buildUpon().setUserRating(ThumbRating(liked)).build()
                )
            }.build()
            session.player.with {
                replaceMediaItem(currentMediaItemIndex, newItem)
            }
            SessionResult(RESULT_SUCCESS, bundleOf("liked" to liked))
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
    ) = scope.future {
        withContext(Dispatchers.Main) {
            mediaSession.player.shuffleModeEnabled = context.recoverShuffle() ?: false
            mediaSession.player.repeatMode = context.recoverRepeat() ?: Player.REPEAT_MODE_OFF
        }
        val (items, index, pos) = context.recoverPlaylist(app, downloadFlow.value)
        MediaItemsWithStartPosition(items, index, pos)
    }

    class ButtonReceiver : MediaButtonReceiver() {
        override fun shouldStartForegroundService(context: Context, intent: Intent): Boolean {
            val isEmpty = context.recoverTracks().isNullOrEmpty()
            if (isEmpty) Toast.makeText(
                context,
                context.getString(R.string.no_last_played_track_found),
                Toast.LENGTH_SHORT
            ).show()
            return !isEmpty
        }
    }

    companion object {
        fun PagedData<Shelf>.toTracks() = map {
            it.getOrThrow().mapNotNull { shelf ->
                when (shelf) {
                    is Shelf.Category -> null
                    is Shelf.Item -> listOfNotNull(shelf.media as? Track)
                    is Shelf.Lists.Categories -> null
                    is Shelf.Lists.Items -> shelf.list.filterIsInstance<Track>()
                    is Shelf.Lists.Tracks -> shelf.list
                }
            }.flatten()
        }
    }
}