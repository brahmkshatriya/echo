package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.clients.LoginClient.CustomTextInput
import dev.brahmkshatriya.echo.common.clients.LoginClient.UsernamePassword
import dev.brahmkshatriya.echo.common.clients.LoginClient.WebView
import dev.brahmkshatriya.echo.common.clients.LoginClient.WebView.Cookie
import dev.brahmkshatriya.echo.common.clients.LoginClient.WebView.Evaluate
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.User

/**
 * To be implemented by the extension to provide login functionality.
 *
 * Do not implement this interface directly, use the sub-interfaces.
 * The extension can implement all of the sub-interfaces.
 *
 * @see [UsernamePassword]
 * @see [WebView]
 * @see [CustomTextInput]
 */
sealed interface LoginClient {

    /**
     * To be implemented when the login screen has username and password fields.
     *
     * @see [CustomTextInput]
     */
    interface UsernamePassword : LoginClient {

        /**
         * Called when the user submits the login form.
         *
         * @param username The username entered by the user
         * @param password The password entered by the user
         *
         * @return A list of users that are logged in
         */
        suspend fun onLogin(username: String, password: String): List<User>
    }

    /**
     * Interface when the login requires a webview.
     *
     * The extension should provide the [loginWebViewInitialUrl] and [loginWebViewStopUrlRegex].
     *
     * Do not implement this interface directly, you can implement either of the sub-interfaces.
     * - [Cookie] - if the login requires cookies
     * - [Evaluate] - if the login requires evaluating javascript
     *
     * Do not implement both the sub-interfaces,
     * [Cookie] will take priority if both are implemented.
     */
    sealed interface WebView : LoginClient {

        /**
         * The initial URL to be loaded in the webview.
         */
        val loginWebViewInitialUrl: Request

        /**
         * The regex to match the URL when the login process is assumed to be complete.
         */
        val loginWebViewStopUrlRegex: Regex

        /**
         * The regex to match the URL to intercept requests made by the webview for cookies.
         */
        val loginWebViewCookieUrlRegex: Regex?
            get() = null

        /**
         * Called when the webview stops loading a URL with the [loginWebViewStopUrlRegex].
         *
         * @param url The URL that the webview stopped at
         * @param data The data from the webview
         *
         * @return A list of users that are logged in
         */
        suspend fun onLoginWebviewStop(url: String, data: Map<String, String>): List<User>

        /**
         * To be implemented when the login requires cookies in [onLoginWebviewStop].
         */
        interface Cookie : WebView

        /**
         * To be implemented when the login requires data after the evaluating javascript in [onLoginWebviewStop].
         */
        interface Evaluate : WebView {

            /**
             * The javascript to be evaluated in the webview.
             * Make sure this a function that can return some data that can be
             * used in the [onLoginWebviewStop] method.
             */
            val javascriptToEvaluate: String
        }
    }

    /**
     * To be implemented when the login screen has custom text input fields.
     *
     * The extension needs to provide the [loginInputFields].
     */
    interface CustomTextInput : LoginClient {

        /**
         * List of input fields to be displayed on the login screen.
         */
        val loginInputFields: List<InputField>

        /**
         * Called when the user submits the login form.
         *
         * @param data A map of the input fields with the key as the `key` from the `InputField` and the value as the user input
         *
         * @return A list of users that are logged in
         */
        suspend fun onLogin(data: Map<String, String?>): List<User>
    }

    /**
     * Represents an input field for the login screen.
     *
     * @param key The key to be used in the `data` map in the `onLogin` method
     * @param label The label to be displayed for the input field
     * @param isRequired If the field is required
     * @param isPassword If the field is a password field
     */
    data class InputField(
        val key: String,
        val label: String,
        val isRequired: Boolean,
        val isPassword: Boolean = false
    )

    /**
     * Called when the extension starts or when user selects a user.
     * `null` if no user is logged in (can also be Incognito mode)
     */
    suspend fun onSetLoginUser(user: User?)

    /**
     * To be called when any other extension needs the current user.
     * Be sure to remove any sensitive data.
     */
    suspend fun getCurrentUser(): User?
}