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
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.run
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    val extensionLoader: ExtensionLoader,
) : ViewModel() {

    private val app = extensionLoader.app
    val messageFlow = app.messageFlow
    private val userDao = extensionLoader.db.userDao()
    val loading = MutableStateFlow(true)
    val loadingOver = MutableSharedFlow<Unit>()

    private suspend fun afterLogin(
        extension: Extension<*>,
        users: List<User>
    ) {
        println("LoginViewModel.afterLogin: $users")
        loading.value = true
        if (users.isEmpty()) {
            app.messageFlow.emit(Message(app.context.getString(R.string.no_user_found)))
            loadingOver.emit(Unit)
            return
        }
        println("LoginViewModel.afterLogin: inserting users into db")
        val entities = users.map { it.toEntity(extension.type, extension.id) }
        userDao.insertUsers(entities)
        println("LoginViewModel.afterLogin: users inserted into db: $entities")
        val user = entities.first()
        userDao.setCurrentUser(user.toCurrentUser())
        println("LoginViewModel.afterLogin: set current user to $user")
        loading.value = false
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        extension: Extension<*>,
        result: Result<List<User>?>,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            println("LoginViewModel.onWebViewStop: result = $result")
            val users = extension.run(app.throwFlow) { result.getOrThrow().orEmpty() }
                ?: return@launch
            println("LoginViewModel.onWebViewStop: users = $users")
            afterLogin(extension, users)
        }
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(
        extension: Extension<*>,
        form: LoginClient.Form
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            println("LoginViewModel.onCustomTextInputSubmit: inputs = $inputs")
            val users = extension.get<LoginClient.CustomInput, List<User>>(app.throwFlow) {
                onLogin(form.key, inputs.toMap())
            } ?: return@launch
            println("LoginViewModel.onCustomTextInputSubmit: users = $users")
            afterLogin(extension, users)
        }
    }
}