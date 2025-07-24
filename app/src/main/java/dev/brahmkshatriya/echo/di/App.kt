package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.common.models.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class App(
    val context: Application,
    val settings: SharedPreferences,
) {
    val throwFlow = MutableSharedFlow<Throwable>()
    val messageFlow = MutableSharedFlow<Message>()
    val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            throwFlow.collectLatest {
                it.printStackTrace()
            }
        }
    }
}
