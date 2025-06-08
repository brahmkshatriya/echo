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
        loading.value = true
        if (users.isEmpty()) {
            app.messageFlow.emit(Message(app.context.getString(R.string.no_user_found)))
            loadingOver.emit(Unit)
            return
        }
        val entities = users.map { it.toEntity(extension.type, extension.id) }
        userDao.insertUsers(entities)
        val user = entities.first()
        userDao.setCurrentUser(user.toCurrentUser())
        loading.value = false
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        extension: Extension<*>,
        result: Result<List<User>?>,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.run(app.throwFlow) { result.getOrThrow().orEmpty() }
                ?: run {
                    loadingOver.emit(Unit)
                    return@launch
                }
            afterLogin(extension, users)
        }
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(
        extension: Extension<*>,
        form: LoginClient.Form
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.CustomInput, List<User>>(app.throwFlow) {
                onLogin(form.key, inputs.toMap())
            } ?: run {
                loadingOver.emit(Unit)
                return@launch
            }
            afterLogin(extension, users)
        }
    }
}