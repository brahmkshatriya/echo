package dev.brahmkshatriya.echo.ui.login

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
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
    private val context: Application,
    val messageFlow: MutableSharedFlow<SnackBar.Message>,
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    val loginClient: MutableStateFlow<LoginClient?> = MutableStateFlow(null)
    private val userDao = database.userDao()
    val loadingOver = MutableSharedFlow<Unit>()

    private suspend fun afterLogin(id: String, users: List<User>?) {
        if (users.isNullOrEmpty()) {
            messageFlow.emit(SnackBar.Message(context.getString(R.string.no_user_found)))
            return
        }
        val entities = users.map { it.toEntity(id) }
        userDao.setUsers(entities)
        userDao.setCurrentUser(entities.first().toCurrentUser())
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        id: String,
        webViewClient: LoginClient.WebView,
        url: String,
        cookie: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = tryWith {
                webViewClient.onLoginWebviewStop(url, cookie)
            }
            afterLogin(id, users)
        }
    }

    fun onUsernamePasswordSubmit(
        id: String,
        client: LoginClient.UsernamePassword,
        username: String,
        password: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = tryWith {
                client.onLogin(username, password)
            }
            afterLogin(id, users)
        }
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(
        id: String,
        client: LoginClient.CustomTextInput,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = tryWith {
                client.onLogin(inputs)
            }
            afterLogin(id, users)
        }
    }
}