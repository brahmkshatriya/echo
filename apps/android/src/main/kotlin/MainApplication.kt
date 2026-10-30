package dev.brahmkshatriya.echo

import android.app.Application
import dev.brahmkshatriya.echo.app.playback.PlaybackComponentOwner
import dev.brahmkshatriya.echo.app.playback.createPlaybackComponent
import dev.brahmkshatriya.kmedianotification.MediaIcon

class MainApplication : Application(), PlaybackComponentOwner {
    override val playbackComponent by lazy {
        createPlaybackComponent(
            this,
            R.drawable.ic_mono,
            configureMedia = {
                buttonIcon = { request ->
                    when (request.icon) {
                        MediaIcon.Play -> R.drawable.ic_play_arrow
                        MediaIcon.Pause -> R.drawable.ic_pause
                        MediaIcon.Previous -> R.drawable.ic_skip_previous
                        MediaIcon.Next ->
                            if (request.enabled) R.drawable.ic_skip_next
                            else R.drawable.ic_skip_next_disabled
                        MediaIcon.Favorite -> R.drawable.ic_favorite
                        MediaIcon.FavoriteFilled -> R.drawable.ic_favorite_filled
                        MediaIcon.RepeatOff -> R.drawable.ic_repeat
                        MediaIcon.RepeatAll -> R.drawable.ic_repeat_on
                        MediaIcon.RepeatOne -> R.drawable.ic_repeat_one_on
                        else -> null
                    }
                }
            },
        )
    }
}
