package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginUserViewModel @Inject constructor(
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: MutableStateFlow<MusicExtension?>
) : CatchingViewModel(throwableFlow) {

    private val userDao = database.userDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMusicUser = extensionFlow.map { extension ->
        userDao.observeCurrentUser(extension?.id).map {
            withContext(Dispatchers.IO) {
                extension to userDao.getUser(extension?.id, it?.id)?.toUser()
            }
        }
    }.flattenConcat()

    val currentExtension = MutableStateFlow<Extension<*>?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser = currentExtension.map { extension ->
        userDao.observeCurrentUser(extension?.id).map {
            withContext(Dispatchers.IO) {
                extension to userDao.getUser(extension?.id, it?.id)?.toUser()
            }
        }
    }.flattenConcat()

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