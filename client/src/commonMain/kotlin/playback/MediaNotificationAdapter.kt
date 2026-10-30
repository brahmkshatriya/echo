package dev.brahmkshatriya.echo.app.playback

import com.skydoves.landscapist.core.Landscapist
import dev.brahmkshatriya.kmedianotification.KMediaNotification
import dev.brahmkshatriya.kmedianotification.MediaAction
import dev.brahmkshatriya.kmedianotification.MediaArtwork
import dev.brahmkshatriya.kmedianotification.MediaButton
import dev.brahmkshatriya.kmedianotification.MediaKind as NotificationMediaKind
import dev.brahmkshatriya.kmedianotification.MediaPlatform
import dev.brahmkshatriya.kmedianotification.MediaSessionEvent
import dev.brahmkshatriya.kmedianotification.MediaSessionState
import dev.brahmkshatriya.kmedianotification.NowPlayingItem
import dev.brahmkshatriya.kmedianotification.PlaybackPhase
import dev.brahmkshatriya.kmedianotification.PlaybackState
import dev.brahmkshatriya.kmedianotification.PositionSample
import dev.brahmkshatriya.kmedianotification.RepeatMode as NotificationRepeatMode
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

private val notificationButtons =
    listOf(
        MediaButton.Previous,
        MediaButton.PlayPause,
        MediaButton.Standard(MediaAction.Next, showWhenUnavailable = true),
        MediaButton.Repeat,
        MediaButton.Like,
    )

/** The only translation between Echo's playback/queue models and OS media sessions. */
class MediaNotificationAdapter(
    private val player: PlayerSession,
    platform: MediaPlatform,
    private val imageLoader: Landscapist,
) : SystemMediaSession {
    override val notification =
        KMediaNotification.create(platform, player.scope, "echo", "Echo") {
            onPlay(player::play)
            onPause(player::pause)
            onTogglePlayPause(player::togglePlayPause)
            onStop(player::clearQueue)
            onNext(player::next)
            onPrevious(player::previous)
            onSeekTo { request ->
                player.seekTo(
                    request.position.inWholeMilliseconds.toFloat(),
                    request.entryId?.toIntOrNull() ?: player.currentEntryId,
                )
            }
            onSeekBy { request ->
                player.activeTimeline?.second?.let { timeline ->
                    val target =
                        (timeline.positionMs + request.offset.inWholeMilliseconds).coerceIn(
                            0f,
                            timeline.durationMs.takeIf { it > 0f } ?: Float.MAX_VALUE,
                        )
                    player.seekTo(target, request.entryId?.toIntOrNull() ?: player.currentEntryId)
                }
            }
            onSkipToEntry { it.toIntOrNull()?.let(player::selectEntry) }
            onSetVolume { player.setVolume(it.toFloat()) }
            onSetLiked { request ->
                val entryId = request.entryId?.toIntOrNull() ?: player.currentEntryId
                player.setFavourite(entryId, request.liked)
            }
            onSetShuffle(player::setShuffle)
            onSetRepeatMode {
                player.repeatMode =
                    when (it) {
                        NotificationRepeatMode.Off -> RepeatMode.Off
                        NotificationRepeatMode.All -> RepeatMode.All
                        NotificationRepeatMode.One -> RepeatMode.One
                    }
            }
        }
    private var previous: SystemMediaState? = null
    private var playback = PlaybackState()
    private var discontinuity = 0L
    private var queueItems: List<NowPlayingItem> = emptyList()
    private val artworkRequest =
        NotificationArtworkRequest(
            scope = player.scope,
            load = imageLoader::loadNotificationArtwork,
            onLoaded = { source, loaded ->
                if (previous?.media?.artworkUri == source) {
                    val current = notification.state.value
                    current.item?.let { item ->
                        // A metadata-only update retains the existing playback clock sample.
                        notification.publish(current.copy(item = item.copy(artwork = loaded)))
                    }
                }
            },
            onError = { player.reportError("Load notification artwork", it) },
        )

    init {
        player.scope.launch {
            notification.events.collect { event ->
                if (event is MediaSessionEvent.Error)
                    player.reportError("System media controls: ${event.operation}", event.cause)
            }
        }
    }

    override fun activate() = notification.activate()

    override fun update(state: SystemMediaState) {
        val source = state.media?.artworkUri
        val artwork = artworkRequest.artwork.takeIf { artworkRequest.source == source }
        val old = previous
        val itemChanged = old?.entryId != state.entryId
        val sampleChanged = old == null || itemChanged || old.positionMs != state.positionMs
        val position =
            if (sampleChanged) state.positionMs.coerceAtLeast(0).milliseconds
            else playback.positionNow()
        if (
            !itemChanged &&
                sampleChanged &&
                abs((position - playback.positionNow()).inWholeMilliseconds) > 1000
        )
            discontinuity++
        val clockChanged =
            old == null ||
                sampleChanged ||
                old.playing != state.playing ||
                old.loading != state.loading ||
                old.speed != state.speed
        playback =
            PlaybackState(
                playWhenReady = state.playing,
                phase = if (state.loading) PlaybackPhase.Buffering else PlaybackPhase.Ready,
                position =
                    if (clockChanged) PositionSample(position, discontinuity = discontinuity)
                    else playback.position,
                duration = state.durationMs.takeIf { it > 0 }?.milliseconds,
                speed = state.speed.takeIf { it > 0 && it.isFinite() } ?: 1.0,
            )
        val actions =
            notification.supportedActions.toMutableSet().apply {
                if (state.media == null) remove(MediaAction.SetLiked)
                if (!state.canGoNext) remove(MediaAction.Next)
                if (!state.canGoPrevious) remove(MediaAction.Previous)
                if (state.durationMs <= 0) {
                    remove(MediaAction.SeekTo)
                    remove(MediaAction.SeekBy)
                }
            }
        if (old?.queue != state.queue)
            queueItems = state.queue.map { it.media.toNotificationItem(it.id.toString()) }
        notification.publish(
            MediaSessionState(
                item =
                    if (old?.media == state.media && old?.entryId == state.entryId)
                        notification.state.value.item
                    else
                        state.media?.let {
                            it.toNotificationItem(state.entryId?.toString() ?: it.id, artwork)
                        },
                playback = playback,
                queue = queueItems,
                availableActions = actions,
                buttons = notificationButtons,
                liked = state.favourite.takeIf { state.media != null },
                volume = state.volume.toDouble(),
                shuffle = state.shuffle,
                repeatMode =
                    when (state.repeatMode) {
                        RepeatMode.Off -> NotificationRepeatMode.Off
                        RepeatMode.All -> NotificationRepeatMode.All
                        RepeatMode.One -> NotificationRepeatMode.One
                    },
            )
        )
        queueItems = notification.state.value.queue
        previous = state
        if (state.media == null) notification.deactivate()
        artworkRequest.update(source, retry = itemChanged)
    }

    override fun close() {
        artworkRequest.close()
        notification.close()
    }
}

private fun PlayableMedia.toNotificationItem(entryId: String, artwork: MediaArtwork? = null) =
    NowPlayingItem(
        mediaId = id,
        entryId = entryId,
        title = title,
        artists = listOfNotNull(artist.takeIf(String::isNotBlank)),
        album = album.takeIf(String::isNotBlank),
        artwork = artwork,
        kind =
            if (kind == MediaKind.Video) NotificationMediaKind.Video
            else NotificationMediaKind.Audio,
    )
