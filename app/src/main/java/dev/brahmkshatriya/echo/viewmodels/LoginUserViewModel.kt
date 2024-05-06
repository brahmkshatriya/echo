package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.dao.UserDao
import dev.brahmkshatriya.echo.models.CurrentUser
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginUserViewModel @Inject constructor(
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: MutableStateFlow<MusicExtension?>,
    private val userFlow: MutableSharedFlow<UserEntity?>,
) : CatchingViewModel(throwableFlow) {

    private val userDao = database.userDao()

    override fun onInitialize() {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.observeCurrentUser().collect { user ->
                val extension = extensionFlow.value ?: return@collect
                if (extension.metadata.id == user?.clientId)
                    setLoginUser(extension.metadata.id, extension.client)
            }
        }
    }

    private suspend fun setLoginUser(id: String, client: ExtensionClient) {
        setLoginUser(id, client, userDao, userFlow, throwableFlow)
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

    fun logout(client: String?, user: User?) {
        if (client == null || user == null) return
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteUser(user.id, client)
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
        suspend fun setLoginUser(
            id: String,
            client: ExtensionClient,
            userDao: UserDao,
            flow: MutableSharedFlow<UserEntity?>,
            throwableFlow: MutableSharedFlow<Throwable>,
        ) = withContext(Dispatchers.IO) {
            if (client !is LoginClient) return@withContext
            val user = userDao.getCurrentUser(id)
            val success = tryWith(throwableFlow) {
                client.onSetLoginUser(user?.toUser())
            }
            if (success != null) flow.emit(user)
        }
    }
}