package dev.brahmkshatriya.echo.common.clients

interface MessagePostClient {
    var postMessage: (String) -> Unit
}