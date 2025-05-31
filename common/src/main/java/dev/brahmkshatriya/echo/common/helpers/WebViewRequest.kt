package dev.brahmkshatriya.echo.common.helpers

import dev.brahmkshatriya.echo.common.helpers.WebViewRequest.Cookie
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest.Evaluate
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest.Headers
import dev.brahmkshatriya.echo.common.models.Request

/**
 * Use this to access the webview from the extension. There are 3 types of requests:
 * - [Headers] - Used to intercept requests made by the webview and get the headers
 * - [Cookie] - Used to get the cookies stored in the webview
 * - [Evaluate] - Used to evaluate javascript in the webview
 *
 * The extension can implement all of the sub-interfaces, and the onStop methods
 * will be called in this order of [Headers], [Cookie], [Evaluate].
 *
 * The [initialUrl] is the URL to be loaded in the webview.
 * The [stopUrlRegex] is the regex to match the URL when the request is assumed to be complete.
 */
sealed interface WebViewRequest<T> {
    /**
     * The initial URL to be loaded in the webview.
     */
    val initialUrl: Request

    /**
     * The regex to match the URL when the request is assumed to be complete.
     * Checks on all requests made by the webview.
     */
    val stopUrlRegex: Regex

    /**
     * The maximum time to wait for data to be returned from the webview.
     * This is in milliseconds.
     */
    val maxTimeout: Long
        get() = 15_000L // Default to 15 seconds

    /**
     * If you want to disable caching of the webview data, set this to true.
     */
    val dontCache: Boolean
        get() = false // Default to false, meaning the webview will cache the data

    /**
     * If you want to get the headers from all requests made by the webview, use this.
     *
     * @see WebViewRequest
     */
    interface Headers<T> : WebViewRequest<T> {
        /**
         * The regex to match the URL to intercept requests made by the webview for the headers.
         */
        val interceptUrlRegex: Regex

        /**
         * Called when the webview stops loading a URL with the [stopUrlRegex]
         *
         * You can convert the data returned from the javascript to [T].
         *
         * @param requests The requests that were intercepted with [interceptUrlRegex]
         *
         * @return The data to be passed to the next step
         */
        suspend fun onStop(requests: List<Request>): T?
    }

    /**
     * If you want to get the cookies stored in the webview, use this.
     *
     * Cookies are cleared when the request is completed.
     *
     * @see WebViewRequest
     */
    interface Cookie<T> : WebViewRequest<T> {
        /**
         * Receive the cookies stored from the webview.
         *
         * You can convert the cookies to [T].
         *
         * @param url The URL that the webview stopped at
         * @param cookie The cookies stored in the webview
         *
         * @return The data to be passed to the next step
         */
        suspend fun onStop(url: Request, cookie: String): T?
    }

    /**
     * If you want to evaluate javascript in the webview, use this.
     *
     * The [javascriptToEvaluate] is evaluated when the webview stops loading a URL with the [stopUrlRegex]
     * and the result is passed to the [onStop] method.
     *
     * You can also use [javascriptToEvaluateOnPageStart] to evaluate javascript when
     * the webview starts loading a URL. Its result is not passed to the [onStop] method.
     *
     * The javascript code should be wrapped in a function that returns some data.
     *
     * @see WebViewRequest
     */
    interface Evaluate<T> : WebViewRequest<T> {

        /**
         * The javascript to be evaluated in the webview.
         * Make sure this js code is wrapped in a function that can return some data.
         */
        val javascriptToEvaluate: String

        /**
         * The javascript to be evaluated when the webview starts loading a URL.
         * This is useful for setting up the webview before it loads the URL.
         *
         * Make sure this js code is wrapped in a function
         */
        val javascriptToEvaluateOnPageStart: String?
            get() = null

        /**
         * Called when the webview stops loading a URL with the [stopUrlRegex] and the javascript
         * is evaluated.
         *
         * You need to convert the data returned from the javascript to [T].
         *
         * @param url The URL that the webview stopped at
         * @param data The data returned from the [javascriptToEvaluate]
         *
         * @return The data to be passed to the next step
         */
        suspend fun onStop(url: Request, data: String?): T?
    }


}