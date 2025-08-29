package dev.brahmkshatriya.echo.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.brahmkshatriya.echo.MainActivity.Companion.getMainActivity
import dev.brahmkshatriya.echo.common.helpers.WebViewClient
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Metadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class WebViewClientFactory(
    private val context: Context,
) {

    val requests = mutableMapOf<Int, Wrapper>()
    val responseFlow = MutableSharedFlow<Pair<Wrapper, Result<String?>?>>()

    suspend fun await(
        ext: Metadata, showWebView: Boolean, reason: String, request: WebViewRequest<String>,
    ): Result<String?> {
        val wrapper = Wrapper(ext, showWebView, reason, request)
        val id = wrapper.hashCode()
        requests[id] = wrapper
        startWebView(id)
        val res = responseFlow.first { it.first == wrapper && it.second != null }.second!!
        requests.remove(id)
        return res
    }

    private fun startWebView(id: Int) {
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, context.getMainActivity()).apply {
                putExtra("webViewRequest", id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ).send()
    }

    fun createFor(metadata: Metadata) = object : WebViewClient {
        override suspend fun await(
            showWebView: Boolean,
            reason: String,
            request: WebViewRequest<String>,
        ): Result<String?> = await(metadata, showWebView, reason, request)
    }

    data class Wrapper(
        val extension: Metadata,
        val showWebView: Boolean,
        val reason: String,
        val request: WebViewRequest<String>,
    )

}