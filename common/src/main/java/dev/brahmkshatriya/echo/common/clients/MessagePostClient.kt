package dev.brahmkshatriya.echo.common.clients

interface MessagePostClient {
    fun postMessage(message: String)
    fun setMessageHandler(handler: (String) -> Unit)
}