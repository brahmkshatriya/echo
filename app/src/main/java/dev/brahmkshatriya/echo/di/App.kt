package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import com.mayakapps.kache.FileKache
import com.mayakapps.kache.KacheStrategy
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.NetworkConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class App(
    val context: Application,
    val settings: SharedPreferences,
) {
    val throwFlow = MutableSharedFlow<Throwable>()
    val messageFlow = MutableSharedFlow<Message>()
    val scope = CoroutineScope(Dispatchers.IO)

    private suspend fun getCache() = FileKache(
        context.cacheDir.resolve("kache").toString(),
        50 * 1024 * 1024
    ) {
        strategy = KacheStrategy.LRU
    }

    val fileCache = scope.async(Dispatchers.IO, CoroutineStart.LAZY) {
        runCatching { getCache() }.getOrElse {
            context.cacheDir.resolve("kache").deleteRecursively()
            getCache()
        }
    }

    private val _networkFlow = MutableStateFlow(NetworkConnection.NotConnected)
    val networkFlow = _networkFlow.asStateFlow()
    val isUnmetered get() = networkFlow.value == NetworkConnection.Unmetered

    init {
        scope.launch {
            throwFlow.collectLatest {
                it.printStackTrace()
            }
        }
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val isMetered = connectivityManager.isActiveNetworkMetered
                _networkFlow.value = if (isMetered) NetworkConnection.Metered
                else NetworkConnection.Unmetered
            }

            override fun onLost(network: Network) {
                _networkFlow.value = NetworkConnection.NotConnected
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }
}
