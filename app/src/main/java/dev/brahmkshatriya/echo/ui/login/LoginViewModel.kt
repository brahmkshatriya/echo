package dev.brahmkshatriya.echo.ui.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
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

    fun onWebViewStop(webViewClient: LoginClient.WebView, cookies: Map<String, String>) {
        viewModelScope.launch {
            val list = tryWith { webViewClient.onLoginWebviewStop(cookies) }
            loginUsers.value = list ?: emptyList()
        }
    }

}