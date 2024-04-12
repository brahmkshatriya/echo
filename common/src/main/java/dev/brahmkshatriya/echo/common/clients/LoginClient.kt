package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.User

sealed interface LoginClient {

    interface UsernamePassword : LoginClient {
        suspend fun onLogin(username: String, password: String): List<User>
    }

    interface WebView : LoginClient {
        val loginWebViewInitialUrl: Request
        val loginWebViewStopUrlRegex: Regex?
        suspend fun onLoginWebviewStop(request: Request, cookie: String): List<User>
    }

    interface CustomTextInput : LoginClient {
        val loginInputFields: List<String>
        suspend fun onLogin(data: Map<String, String>): List<User>
    }

    suspend fun onSetLoginUser(user: User)
}