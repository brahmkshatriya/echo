package dev.brahmkshatriya.echo.extension

import android.annotation.SuppressLint
import android.app.Application
import java.io.File

@Suppress("unused")
class ADSpotifyExtension : SpotifyExtension() {

    @SuppressLint("PrivateApi")
    private fun getApplication(): Application {
        return Class.forName("android.app.ActivityThread").getMethod("currentApplication")
            .invoke(null) as Application
    }

    override val filesDir by lazy { File(getApplication().filesDir, "spotify") }
    
    // Using OGG streaming via Mercury protocol instead of Widevine
    // This works for all logged-in users
    override val showWidevineStreams = false
}
