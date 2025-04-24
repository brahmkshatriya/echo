package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLiked

object PlayerCommands {
    val likeCommand = SessionCommand("liked", Bundle.EMPTY)
    val unlikeCommand = SessionCommand("unliked", Bundle.EMPTY)
    val repeatCommand = SessionCommand("repeat", Bundle.EMPTY)
    val repeatOffCommand = SessionCommand("repeat_off", Bundle.EMPTY)
    val repeatOneCommand = SessionCommand("repeat_one", Bundle.EMPTY)
    val playCommand = SessionCommand("play", Bundle.EMPTY)
    val addToQueueCommand = SessionCommand("add_to_queue", Bundle.EMPTY)
    val addToNextCommand = SessionCommand("add_to_next", Bundle.EMPTY)
    val radioCommand = SessionCommand("radio", Bundle.EMPTY)
    val sleepTimer = SessionCommand("sleep_timer", Bundle.EMPTY)
    val resumeCommand = SessionCommand("resume", Bundle.EMPTY)
    val imageCommand = SessionCommand("image", Bundle.EMPTY)

    fun getLikeButton(context: Context, item: MediaItem) = run {
        val builder = CommandButton.Builder()
        if (!item.isLiked) builder
            .setDisplayName(context.getString(R.string.like))
            .setIconResId(R.drawable.ic_favorite_20dp)
            .setSessionCommand(likeCommand)
        else builder
            .setDisplayName(context.getString(R.string.unlike))
            .setIconResId(R.drawable.ic_favorite_filled_20dp)
            .setSessionCommand(unlikeCommand)
        builder.build()
    }

    fun getRepeatButton(context: Context, repeat: Int) = run {
        val builder = CommandButton.Builder()
        when (repeat) {
            Player.REPEAT_MODE_ONE -> builder
                .setDisplayName(context.getString(R.string.repeat_one))
                .setIconResId(R.drawable.ic_repeat_one_20dp)
                .setSessionCommand(repeatOffCommand)

            Player.REPEAT_MODE_OFF -> builder
                .setDisplayName(context.getString(R.string.repeat_off))
                .setIconResId(R.drawable.ic_repeat_20dp)
                .setSessionCommand(repeatCommand)

            else -> builder
                .setDisplayName(context.getString(R.string.repeat_all))
                .setIconResId(R.drawable.ic_repeat_on_20dp)
                .setSessionCommand(repeatOneCommand)
        }
        builder.build()
    }
}