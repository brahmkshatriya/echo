package dev.brahmkshatriya.echo.ui.player.quality

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.R

object FormatUtils {
    @OptIn(UnstableApi::class)
    private fun Format.getBitrate() =
        (bitrate / 1000).takeIf { it > 0 }?.let { " • $it kbps" } ?: ""

    private fun Format.getFrameRate() =
        frameRate.toInt().takeIf { it > 0 }?.let { " • $it fps" } ?: ""


    private fun Format.getMimeType() = when (val mime = sampleMimeType?.replace("audio/", "")) {
        "mp4a-latm" -> "AAC"
        else -> mime?.uppercase()
    }

    private fun Format.getHertz() =
        sampleRate.takeIf { it > 0 }?.let { " • $it Hz" } ?: ""

    private fun Format.getChannelCount() =
        channelCount.takeIf { it > 0 }?.let { " • ${it}ch" } ?: ""

    @OptIn(UnstableApi::class)
    fun Format.toAudioDetails() =
        "${getMimeType()}${getHertz()}${getChannelCount()}${getBitrate()}"

    fun Format.toVideoDetails() = "${height}p${getFrameRate()}${getBitrate()}"
    fun Format.toSubtitleDetails() = label ?: language ?: "Unknown"

    private fun List<Tracks.Group>.getSelectedFormat(): Format? {
        return firstNotNullOfOrNull { trackGroup ->
            val index = (0 until trackGroup.length).firstNotNullOfOrNull { i ->
                if (trackGroup.isTrackSelected(i)) i else null
            } ?: return null
            trackGroup.getTrackFormat(index)
        }
    }

    fun List<Tracks.Group>.getSelected(): Pair<List<Pair<Tracks.Group, Int>>, Int?> {
        var selected: Pair<Tracks.Group, Int>? = null
        val trackGroups = map { trackGroup ->
            (0 until trackGroup.length).map { i ->
                val pair = Pair(trackGroup, i)
                val isSelected = trackGroup.isTrackSelected(i)
                if (isSelected) selected = pair
                pair
            }
        }.flatten()
        val select = trackGroups.indexOf(selected).takeIf { it != -1 }
        return trackGroups to select
    }

    fun Tracks.getDetails(context: Context): List<String> {
        val audios = groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val videos = groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        val subtitles = groups.filter { it.type == C.TRACK_TYPE_TEXT }
        return listOfNotNull(
            audios.getSelectedFormat()?.toAudioDetails(),
            videos.getSelectedFormat()?.toVideoDetails(),
            subtitles.getSelectedFormat()?.toSubtitleDetails()
        ).ifEmpty { listOf(context.getString(R.string.unknown_quality)) }
    }
}
