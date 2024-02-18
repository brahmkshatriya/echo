package dev.brahmkshatriya.echo.common.clients

import android.content.Context
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata

abstract class ExtensionClient {
    lateinit var context: Context
    abstract fun getMetadata(): ExtensionMetadata
}