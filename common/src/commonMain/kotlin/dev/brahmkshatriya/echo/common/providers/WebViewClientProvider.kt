package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.helpers.WebViewClient

interface WebViewClientProvider {
    fun setWebViewClient(webViewClient: WebViewClient)
}