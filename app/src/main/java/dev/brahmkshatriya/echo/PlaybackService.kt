package dev.brahmkshatriya.echo

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.paging.AsyncPagingDataDiffer
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.data.clients.SearchClient
import dev.brahmkshatriya.echo.data.clients.TrackClient
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import dev.brahmkshatriya.echo.data.models.MediaItemsContainer
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.mediaItemBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var extension: OfflineExtension

    private var mediaLibrarySession: MediaLibrarySession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .build()

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, Callback(extension))
            .setSessionActivity(pendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider
            .Builder(this)
            .setChannelName(R.string.app_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)

        setMediaNotificationProvider(notificationProvider)
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    inner class Callback(
        private val extension: Any
    ) : MediaLibrarySession.Callback {

        private val scope = CoroutineScope(Dispatchers.IO) + Job()

        private fun <T> notSupported() = Futures.immediateFuture(
            LibraryResult.ofError<T>(LibraryResult.RESULT_ERROR_NOT_SUPPORTED)
        )

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (extension !is SearchClient) return notSupported()
            if (extension !is TrackClient) return notSupported()

            return scope.future(Dispatchers.IO) {
                val differ = AsyncPagingDataDiffer(
                    MediaItemsContainerAdapter.MediaItemsContainerComparator,
                    MediaItemsContainerAdapter.ListCallback(),
                )
                extension.search(query).map {
                    differ.submitData(it)
                }
                val list = differ.snapshot().items.map {
                    val track = (it as? MediaItemsContainer.TrackItem)?.track ?: return@map null
                    val stream = extension.getStreamable(track)
                    mediaItemBuilder(track, stream)
                }.filterNotNull()
                LibraryResult.ofItemList(list, params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            if (extension !is TrackClient) return notSupported()
            return scope.future(Dispatchers.IO) {
                val track = extension.getTrack(mediaId)
                    ?: return@future LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
                val stream = extension.getStreamable(track)
                val item = mediaItemBuilder(track, stream)
                LibraryResult.ofItem(item, null)
            }
        }
    }
}