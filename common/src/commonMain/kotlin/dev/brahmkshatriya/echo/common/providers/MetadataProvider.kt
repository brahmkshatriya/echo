package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.models.Metadata

interface MetadataProvider {
    fun setMetadata(metadata: Metadata)
}