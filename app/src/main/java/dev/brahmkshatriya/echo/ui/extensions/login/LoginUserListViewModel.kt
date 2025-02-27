package dev.brahmkshatriya.echo.ui.extensions.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toCurrentUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginUserListViewModel(
    val extensionLoader: ExtensionLoader,
) : ViewModel() {

    private val userDao = extensionLoader.db.userDao()
    val currentExtension = MutableStateFlow<Extension<*>?>(null)
    val currentUser = MutableStateFlow<Pair<Extension<*>?, User?>>(null to null)

    init {
        suspend fun update() {
            val currentExt = currentExtension.value
            val user = withContext(Dispatchers.IO) { userDao.getCurrentUser(currentExt?.id) }
            currentUser.value = currentExt to user?.user
        }
        viewModelScope.launch {
            launch { currentExtension.collect { update() } }
            userDao.observeCurrentUser().collect { update() }
        }
    }

    val allUsers = currentExtension.map { extension ->
        withContext(Dispatchers.IO) {
            extension to extension?.let { ext ->
                userDao.getAllUsers(ext.type, ext.id).map { it.user }
            }
        }
    }

    fun logout(user: UserEntity?) {
        if (user == null) return
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteUser(user)
        }
    }

    fun setLoginUser(user: UserEntity?) {
        val currentUser = user?.toCurrentUser()
            ?: currentExtension.value?.let { CurrentUser(it.type, it.id, null) }
            ?: return
        setLoginUser(currentUser)
    }

    fun setLoginUser(currentUser: CurrentUser) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.setCurrentUser(currentUser)
        }
    }
}