package dev.brahmkshatriya.echo.ui.extensions.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toCurrentUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginUserListViewModel(
    val extensionLoader: ExtensionLoader,
) : ViewModel() {

    private val userDao = extensionLoader.db.userDao()
    val currentExtension = MutableStateFlow<Extension<*>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allUsers = currentExtension
        .combine(extensionLoader.all) { ext, all ->
            if (ext == null) return@combine null
            all.find { it.type == ext.type && it.id == ext.id }
        }
        .combine(userDao.observeCurrentUser()) { a, b -> a to b }
        .flatMapLatest { (ext, current) ->
            val curr = current.find { it.extId == ext?.id && it.type == ext.type }
            val flow = if (ext != null) userDao.observeAllUsers(ext.type, ext.id)
            else null
            flow?.map {
                val users = it.map { ent ->
                    val selected = ent.id == curr?.userId
                    ent.user to selected
                }
                ext to users
            } ?: flow { }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null to listOf())

    val currentUser = allUsers.map {
        it.second.find { user -> user.second }?.first
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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