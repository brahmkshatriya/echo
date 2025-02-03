package dev.brahmkshatriya.echo.ui.login

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val extensionList: MutableStateFlow<List<MusicExtension>?>,
    val trackerList: MutableStateFlow<List<TrackerExtension>?>,
    val lyricsList: MutableStateFlow<List<LyricsExtension>?>,
    val miscList: MutableStateFlow<List<MiscExtension>?>,
    private val context: Application,
    val messageFlow: MutableSharedFlow<Message>,
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    val loginClient: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val userDao = database.userDao()
    val loadingOver = MutableSharedFlow<Unit>()

    private suspend fun afterLogin(
        extension: Extension<*>,
        users: List<User>
    ) {
        if (users.isEmpty()) {
            messageFlow.emit(Message(context.getString(R.string.no_user_found)))
            return
        }
        val entities = users.map { it.toEntity(extension.id) }
        userDao.setUsers(entities)
        val user = entities.first()
        userDao.setCurrentUser(user.toCurrentUser())
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        extension: Extension<*>,
        url: String,
        cookie: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.WebView, List<User>>(throwableFlow) {
                onLoginWebviewStop(url, cookie)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }

    fun onUsernamePasswordSubmit(
        extension: Extension<*>,
        username: String,
        password: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.UsernamePassword, List<User>>(throwableFlow) {
                onLogin(username, password)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(
        extension: Extension<*>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = extension.get<LoginClient.CustomTextInput, List<User>>(throwableFlow) {
                onLogin(inputs)
            } ?: return@launch
            afterLogin(extension, users)
        }
    }
}