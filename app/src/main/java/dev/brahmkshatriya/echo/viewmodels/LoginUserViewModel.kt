package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.dao.UserDao
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
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: ExtensionModule.ExtensionFlow,
    private val userFlow: MutableSharedFlow<UserEntity?>,
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
        setLoginUser(client, userDao, userFlow)
    }

    val currentUser = extensionFlow.combine(userDao.observeCurrentUser()) { client, user ->
        coroutineScope {
            withContext(Dispatchers.IO) {
                client to userDao.getUser(client?.metadata?.id, user?.id)?.toUser()
            }
        }
    }

    val allUsers = extensionFlow.map { client ->
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

    companion object {
        suspend fun CatchingViewModel.setLoginUser(
            client: ExtensionClient?,
            userDao: UserDao,
            flow: MutableSharedFlow<UserEntity?>
        ) {
            if (client is LoginClient) {
                val user = coroutineScope {
                    withContext(Dispatchers.IO) { userDao.getCurrentUser(client.metadata.id) }
                }
                val success =
                    tryWith { client.onSetLoginUser(user?.toUser())}
                if (success != null) flow.emit(user)
            }
        }
    }
}