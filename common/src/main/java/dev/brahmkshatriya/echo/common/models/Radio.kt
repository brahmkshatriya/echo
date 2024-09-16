package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Radio(
    val id: String,
    val title: String,
    val cover: ImageHolder? = null,
    val tabs: List<Tab?> = listOf(),
    val extras: Map<String, String> = mapOf()
)