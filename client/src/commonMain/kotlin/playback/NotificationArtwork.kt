package dev.brahmkshatriya.echo.app.playback

import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.model.ImageResult
import com.skydoves.landscapist.image.convertToImageBitmap
import dev.brahmkshatriya.echo.app.platform.toByteArray
import dev.brahmkshatriya.kmedianotification.MediaArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val NotificationArtworkSize = 512

suspend fun Landscapist.loadNotificationArtwork(model: String): MediaArtwork {
    val result = load(ImageRequest.builder().model(model).size(NotificationArtworkSize, NotificationArtworkSize).build())
        .first { it is ImageResult.Failure || it is ImageResult.Success && it.isFinal }
    if (result is ImageResult.Failure) throw result.throwable ?: IllegalStateException(result.message ?: "Artwork request failed")
    result as ImageResult.Success
    return withContext(Dispatchers.Default) {
        val image = checkNotNull(convertToImageBitmap(result.data)) { "Unsupported notification artwork image" }
        MediaArtwork(image.toByteArray(quality = 90, maxSize = NotificationArtworkSize), "image/jpeg")
    }
}
