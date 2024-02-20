package dev.brahmkshatriya.echo.di

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


// Dagger cannot directly infer Foo<Bar>, if Bar is an interface
// That means the Flow<Clients?> cannot be directly injected,
// So, we need to wrap it in a data class and inject that instead
data class MutableExtensionFlow(val flow: MutableStateFlow<ExtensionClient?>)
data class ExtensionFlow(val flow: Flow<ExtensionClient?>)
data class SearchFlow(val flow: Flow<SearchClient?>)
data class HomeFeedFlow(val flow: Flow<HomeFeedClient?>)
data class TrackFlow(val flow: Flow<TrackClient?>)