package dev.brahmkshatriya.echo.extensions.plugger.interfaces

interface ManifestParser<TInputData, TMetadata> {
    fun parseManifest(data: TInputData): TMetadata
}