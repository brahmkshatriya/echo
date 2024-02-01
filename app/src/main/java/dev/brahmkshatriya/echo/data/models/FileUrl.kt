package dev.brahmkshatriya.echo.data.models

data class FileUrl(
    val url: String,
    val headers: Map<String, String> = mapOf()
)

fun String?.toFileUrl() : FileUrl? {
    return FileUrl(this
        ?: return null)
}