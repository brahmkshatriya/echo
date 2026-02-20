package dev.brahmkshatriya.echo.extensions.builtin.offline

import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File


fun getLyricsFromAudioPath(audioPath: String): String? {
    Log.i("[getLyricsFromAudioPath]", "getLyricsFromAudioPath entered with audio path $audioPath")
    // first check for .lrc file
    val lrcFile = File(audioPath).let {
        File(it.parentFile, "${it.nameWithoutExtension}.lrc")
    }
    if (lrcFile.exists()) {
        Log.i("[getLyricsFromAudioPath]", "Found LRC file, reading text and returning")
        return lrcFile.readText(Charsets.UTF_8)
    }

    // fall back to embedded lyrics
    return try {
        Log.i("[getLyricsFromAudioPath]", "LRC file not found, loading file into memory and reading metadata")
        val audioFile = AudioFileIO.read(File(audioPath))
        Log.i("[getLyricsFromAudioPath]", "File read into memory, grabbing metadata")
        audioFile.tag?.getFirst(FieldKey.LYRICS).also {
            Log.i("[getLyricsFromAudioPath]", "Found lyrics in file, length = ${it?.length}")
        }
    } catch (e: Exception) {
        null
    }
}