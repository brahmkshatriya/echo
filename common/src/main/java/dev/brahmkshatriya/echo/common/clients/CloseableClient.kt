package dev.brahmkshatriya.echo.common.clients

interface CloseableClient {
    /**
     * Called when the app and player are closed.
     * Useful for cleaning up resources.
     */
    fun close()
}