package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.SharedPreferences
import com.mayakapps.kache.FileKache
import com.mayakapps.kache.KacheStrategy
import dev.brahmkshatriya.echo.common.models.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    val fileCache = scope.async(Dispatchers.IO, CoroutineStart.LAZY) {
        FileKache(
            context.cacheDir.resolve("kache").toString(),
            100 * 1024 * 1024
        ) {
            strategy = KacheStrategy.LRU
        }
    }

    init {
        scope.launch {
            throwFlow.collectLatest {
                it.printStackTrace()
            }
        }
    }
}
