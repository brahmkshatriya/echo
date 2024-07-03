package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoApplication.Companion.TIMEOUT
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
            userDao.observeCurrentUser().collect { list ->
                val extension = extensionFlow.value ?: return@collect
                val user = list.find { it.clientId == extension.metadata.id }
                if (extension.metadata.id == user?.clientId)
                    setLoginUser(extension.metadata.id, extension.client)
            }
        }
    }

    private suspend fun setLoginUser(id: String, client: ExtensionClient) {
        setLoginUser(id, client, userDao, userFlow, throwableFlow)
    }

    val currentMusicUser = extensionFlow.combine(userDao.observeCurrentUser()) { extension, list ->
        val id = extension?.metadata?.id
        val user = list.find { it.clientId == id }
        withContext(Dispatchers.IO) {
            extension to userDao.getUser(id, user?.id)?.toUser()
        }
    }

    val currentExtension = MutableStateFlow<Pair<ExtensionMetadata, ExtensionClient>?>(null)
    val currentUser = currentExtension.combine(userDao.observeCurrentUser()) { it, list ->
        val metadata = it?.first
        val client = it?.second
        val user = list.find { it.clientId == metadata?.id }
        withContext(Dispatchers.IO) {
            Triple(
                metadata,
                client,
                userDao.getUser(metadata?.id, user?.id)?.toUser()
            )
        }
    }

    val allUsers = currentExtension.map { it ->
        val metadata = it?.first
        withContext(Dispatchers.IO) {
            metadata to metadata?.id?.let { id ->
                userDao.getAllUsers(id).map { it.toUser() }
            }
        }
    }

    fun logout(client: String?, user: String?) {
        if (client == null || user == null) return
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteUser(user, client)
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
            println("$id user : $user")
            val success = tryWith(throwableFlow) {
                withTimeout(TIMEOUT) { client.onSetLoginUser(user?.toUser()) }
            }
            if (success != null) flow.emit(user)
        }
    }
}