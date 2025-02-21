package dev.brahmkshatriya.echo.extensions.plugger.interfaces

import kotlinx.coroutines.flow.StateFlow

interface PluginSource<TSourceInput> {
    fun getSourceFiles() : StateFlow<List<TSourceInput>>
}