package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

interface LibraryClient {
    suspend fun getLibraryTabs(): List<Tab>
    fun getLibraryFeed(tab: Tab?): PagedData<Shelf>
}