package dev.brahmkshatriya.echo.common.helpers

data class Page<T : Any, P>(
    val data: List<T>,
    val continuation: P?
)