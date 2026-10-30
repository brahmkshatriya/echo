package dev.brahmkshatriya.echo.app.playback

/** Attempt every release, then propagate the first failure with the others attached. */
fun closeAll(vararg releases: () -> Unit) {
    var failure: Throwable? = null
    for (release in releases) try {
        release()
    } catch (error: Throwable) {
        val previous = failure
        if (previous == null) failure = error
        else if (previous !== error) previous.addSuppressed(error)
    }
    failure?.let { throw it }
}
