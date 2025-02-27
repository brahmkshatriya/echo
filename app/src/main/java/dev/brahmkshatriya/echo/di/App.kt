package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.common.models.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class App(
    val context: Application,
    val settings: SharedPreferences,
) {
    val throwFlow = MutableSharedFlow<Throwable>()
    val messageFlow = MutableSharedFlow<Message>()

    init {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch {
            throwFlow.collectLatest {
                it.printStackTrace()
            }
        }
    }
}
