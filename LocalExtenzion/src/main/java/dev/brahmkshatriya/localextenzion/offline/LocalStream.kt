package dev.brahmkshatriya.echo.common.data.offline

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.brahmkshatriya.echo.common.models.Track

interface LocalStream {
    companion object {
        fun getFromTrack(context: Context, track: Track): Uri? {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.DATA),
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(track.uri.lastPathSegment!!),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return Uri.parse(it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)))
                }
            }
            return null
        }
    }
}