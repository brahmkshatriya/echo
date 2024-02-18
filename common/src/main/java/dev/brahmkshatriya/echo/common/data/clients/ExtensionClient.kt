package dev.brahmkshatriya.echo.common.data.clients

import android.content.Context
import dev.brahmkshatriya.echo.common.data.models.ExtensionMetadata

abstract class ExtensionClient {
    lateinit var context: Context
    abstract fun getMetadata(): ExtensionMetadata
}