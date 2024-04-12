package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.UrlHolder
import dev.brahmkshatriya.echo.common.models.User

sealed interface LoginClient {

    interface UsernamePassword : LoginClient {
        suspend fun onLogin(username: String, password: String): List<User>
    }

    interface WebView : LoginClient {
        val loginWebViewInitialUrl: UrlHolder
        val loginWebViewStopUrlRegex: Regex
        suspend fun onLoginWebviewStop(url: String, cookies: Map<String, String>): List<User>
    }

    interface CustomTextInput : LoginClient {
        val loginInputFields: List<String>
        suspend fun onLogin(data: Map<String, String>): List<User>
    }

    suspend fun onSetLoginUser(user: User)
}