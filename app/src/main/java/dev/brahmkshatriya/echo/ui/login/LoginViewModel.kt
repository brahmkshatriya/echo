package dev.brahmkshatriya.echo.ui.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val extensionList: ExtensionModule.ExtensionListFlow,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    val loginUsers: MutableStateFlow<List<User>?> = MutableStateFlow(null)

    fun onWebViewStop(
        webViewClient: LoginClient.WebView,
        url: String,
        cookie: String
    ) {
        println("Cookie : $cookie")
        viewModelScope.launch(Dispatchers.IO) {
            val list = tryWith {
                webViewClient.onLoginWebviewStop(url, cookie)
            }
            loginUsers.value = list ?: emptyList()
        }
    }

    fun onUserSelected(client: LoginClient, user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            tryWith { client.onSetLoginUser(user) }
        }
    }
}