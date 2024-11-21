package dev.brahmkshatriya.echo.common.clients

abstract class MessagePostClient {
    var postMessage: (String) -> Unit = {}
}