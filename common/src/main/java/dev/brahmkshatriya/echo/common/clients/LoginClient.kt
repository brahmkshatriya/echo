package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.User

sealed interface LoginClient {

    interface UsernamePassword : LoginClient {
        suspend fun onLogin(username: String, password: String): List<User>
    }

    sealed interface WebView : LoginClient {
        val loginWebViewInitialUrl: Request
        val loginWebViewStopUrlRegex: Regex
        suspend fun onLoginWebviewStop(url: String, data: String): List<User>
        interface Cookie : WebView
        interface Evaluate : WebView {
            val javascriptToEvaluate: String
        }
    }

    interface CustomTextInput : LoginClient {
        val loginInputFields: List<InputField>
        suspend fun onLogin(data: Map<String, String?>): List<User>
    }

    data class InputField(
        val key: String,
        val label: String,
        val isRequired: Boolean,
        val isPassword: Boolean = false
    )

    suspend fun onSetLoginUser(user: User?)

    //Will be only used by other clients, be sure to remove any sensitive data
    suspend fun getCurrentUser(): User?
}