package dev.brahmkshatriya.echo.ui.extensions.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    val extensionLoader: ExtensionLoader,
) : ViewModel() {

    val loginClient: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val app = extensionLoader.app
    val messageFlow = app.messageFlow
    private val userDao = extensionLoader.db.userDao()
    val loadingOver = MutableSharedFlow<Unit>()

    private suspend fun afterLogin(
        extension: Extension<*>,
        users: List<User>
    ) {
        if (users.isEmpty()) {
            app.messageFlow.emit(Message(app.context.getString(R.string.no_user_found)))
            return
        }
        val entities = users.map { it.toEntity(extension.type, extension.id) }
        userDao.insertUsers(entities)
        val user = entities.first()
        userDao.setCurrentUser(user.toCurrentUser())
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        extension: Extension<*>,
        url: String,
        cookie: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.WebView, List<User>>(app.throwFlow) {
                onLoginWebviewStop(url, cookie)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }

    fun onUsernamePasswordSubmit(
        extension: Extension<*>,
        username: String,
        password: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.UsernamePassword, List<User>>(app.throwFlow) {
                onLogin(username, password)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(
        extension: Extension<*>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.CustomTextInput, List<User>>(app.throwFlow) {
                onLogin(inputs)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }
}