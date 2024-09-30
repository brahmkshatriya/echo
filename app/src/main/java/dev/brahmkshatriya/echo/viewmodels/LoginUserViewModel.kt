package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginUserViewModel @Inject constructor(
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: MutableStateFlow<MusicExtension?>,
    val extensionLoader: ExtensionLoader,
) : CatchingViewModel(throwableFlow) {

    private val userDao = database.userDao()
    val currentExtension = MutableStateFlow<Extension<*>?>(null)
    val currentUser = MutableStateFlow<Pair<Extension<*>?, User?>>(null to null)

    init {
        var users = emptyList<UserEntity>()
        fun update() {
            val currentExt = currentExtension.value
            val entities = users
            val current = entities.find { it.id == currentExt?.id }
            currentUser.value = currentExt to current?.toUser()
        }
        viewModelScope.launch {
            launch { currentExtension.collect { update() } }
            userDao.observeCurrentUser().collect {
                users = it
                update()
            }
        }
    }

    val allUsers = currentExtension.map { extensionData ->
        val metadata = extensionData?.metadata
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
        setLoginUser(currentUser)
    }

    fun setLoginUser(currentUser: CurrentUser) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.setCurrentUser(currentUser)
        }
    }

}