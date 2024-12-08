package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.providers.SettingsProvider
import okhttp3.OkHttpClient

/**
 * Interface to be implemented by every [Extension], create your network client, database client, etc. here.
 *
 * If using [OkHttpClient], use the custom [await] function for suspending the network call.
 * ```kotlin
 * val request = Request.Builder().url("https://example.com").build()
 * val response = client.newCall(request).await()
 * ```
 *
 * This interface extends the [SettingsProvider] interface
 */
interface ExtensionClient : SettingsProvider {
    /**
     * When an extension is selected by the user, prefer using this method over the `init` initializer
     *
     * can be called multiple times, if the user re-selects the extension
     */
    suspend fun onExtensionSelected()
}