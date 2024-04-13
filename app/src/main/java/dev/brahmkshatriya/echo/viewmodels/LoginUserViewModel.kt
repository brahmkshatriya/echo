package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.models.CurrentUser
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginUserViewModel @Inject constructor(
    database: EchoDatabase,
    val extensionFlow: ExtensionModule.ExtensionFlow,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    private val userDao = database.userDao()

    override fun onInitialize() {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.observeCurrentUser().collect { user ->
                val client = extensionFlow.value ?: return@collect
                if (client.metadata.id == user?.clientId)
                    tryWith { setLoginUser(client) }
            }
        }
    }

    private suspend fun setLoginUser(client: ExtensionClient?) {
        if (client !is LoginClient) return
        val user = userDao.getCurrentUser(client.metadata.id) ?: return
        tryWith { client.onSetLoginUser(user.toUser()) }
    }

    val currentUser
        get() = userDao.observeCurrentUser().combine(extensionFlow) { user, client ->
            coroutineScope {
                withContext(Dispatchers.IO) {
                    client to userDao.getUser(client?.metadata?.id, user?.id)?.toUser()
                }
            }
        }

    val allUsers
        get() = extensionFlow.map { client ->
            coroutineScope {
                withContext(Dispatchers.IO) {
                    client to client?.metadata?.id?.let { id ->
                        userDao.getAllUsers(id).map { it.toUser() }
                    }
                }
            }
        }

    fun setLoginUser(user: UserEntity?) {
        val currentUser = user?.toCurrentUser()
            ?: CurrentUser(extensionFlow.value?.metadata?.id ?: return, null)
        viewModelScope.launch(Dispatchers.IO) {
            userDao.setCurrentUser(currentUser)
        }
    }
}