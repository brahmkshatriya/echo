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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginUserListViewModel(
    val extensionLoader: ExtensionLoader,
) : ViewModel() {

    private val userDao = extensionLoader.db.userDao()
    val currentExtension = MutableStateFlow<Extension<*>?>(null)
    val allUsers = MutableStateFlow<Pair<Extension<*>?, List<User>?>>(null to null)
    val currentUser = MutableStateFlow<Pair<Extension<*>?, User?>>(null to null)

    init {
        suspend fun update() {
            val currentExt = currentExtension.value
            val users = currentExt?.let {
                withContext(Dispatchers.IO) { userDao.getAllUsers(it.type, it.id) }
            }?.map { it.user }
            val user = currentExt?.let {
                withContext(Dispatchers.IO) { userDao.getCurrentUser(it.type, it.id) }
            }?.let { curr -> users?.find { it.id == curr.userId } }
            currentUser.value = currentExt to user
            allUsers.value = currentExt to users
        }
        viewModelScope.launch {
            launch { currentExtension.collect { update() } }
            userDao.observeCurrentUser().collect { update() }
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