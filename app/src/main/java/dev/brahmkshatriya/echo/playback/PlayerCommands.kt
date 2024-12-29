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
    val radioCommand = SessionCommand("radio", Bundle.EMPTY)
    val sleepTimer = SessionCommand("sleep_timer", Bundle.EMPTY)

    fun getLikeButton(context: Context, item: MediaItem) = run {
        val builder = CommandButton.Builder()
        if (!item.isLiked) builder
            .setDisplayName(context.getString(R.string.like))
            .setIconResId(R.drawable.ic_heart_outline)
            .setSessionCommand(likeCommand)
        else builder
            .setDisplayName(context.getString(R.string.unlike))
            .setIconResId(R.drawable.ic_heart_filled)
            .setSessionCommand(unlikeCommand)
        builder.build()
    }

    fun getRepeatButton(context: Context, repeat: Int) = run {
        val builder = CommandButton.Builder()
        builder.setDisplayName(context.getString(R.string.repeat))
        when (repeat) {
            Player.REPEAT_MODE_ONE -> builder
                .setIconResId(R.drawable.ic_repeat_one)
                .setSessionCommand(repeatOffCommand)

            Player.REPEAT_MODE_OFF -> builder
                .setIconResId(R.drawable.ic_repeat_off)
                .setSessionCommand(repeatCommand)

            else -> builder
                .setIconResId(R.drawable.ic_repeat)
                .setSessionCommand(repeatOneCommand)
        }
        builder.build()
    }
}