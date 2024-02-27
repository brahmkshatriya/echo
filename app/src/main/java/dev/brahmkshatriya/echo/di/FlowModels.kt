package dev.brahmkshatriya.echo.di

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


// Dagger cannot directly infer Foo<Bar>, if Bar is an interface
// That means the Flow<Clients?> cannot be directly injected,
// So, we need to wrap it in a data class and inject that instead
data class MutableExtensionFlow(val flow: MutableStateFlow<ExtensionClient?>)
data class ExtensionFlow(val flow: Flow<ExtensionClient?>)