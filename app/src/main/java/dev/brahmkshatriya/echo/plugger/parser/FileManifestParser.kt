package dev.brahmkshatriya.echo.plugger.parser

import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import tel.jeelpa.plugger.ManifestParser
import java.io.File

class FileManifestParser : ManifestParser<File, ExtensionMetadata> {
    override fun parseManifest(data: File): ExtensionMetadata {
        TODO("Not yet implemented")
    }
}