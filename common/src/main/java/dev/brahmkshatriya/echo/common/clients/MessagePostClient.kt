package dev.brahmkshatriya.echo.common.clients

interface MessagePostClient {
    /**
     * Posts a message to the user.
     * Call this when you want to display a notification to the user.
     *
     * @param message The message text to display
     */
    fun postMessage(message: String)

    /**
     * Internal setup method used by the main app.
     * Extensions should not call this method.
     */
    fun setMessageHandler(handler: (String) -> Unit)
}