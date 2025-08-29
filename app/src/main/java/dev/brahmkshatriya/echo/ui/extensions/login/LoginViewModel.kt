package dev.brahmkshatriya.echo.ui.extensions.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionFlow
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    val extensionLoader: ExtensionLoader,
    val extensionType: ExtensionType,
    val extensionId: String? = null,
) : ViewModel() {
    val extension = extensionLoader.getFlow(extensionType).getExtensionFlow(extensionId)
        .stateIn(viewModelScope, Eagerly, null)

    private val app = extensionLoader.app
    val messageFlow = app.messageFlow
    private val userDao = extensionLoader.db.userDao()
    val loading = MutableStateFlow(true)
    val loadingOver = MutableSharedFlow<Unit>()
    val addFragmentFlow = MutableSharedFlow<FragmentType>()

    sealed class FragmentType {
        data object Selector : FragmentType()
        data object WebView : FragmentType()
        data class CustomInput(val index: Int?) : FragmentType()
    }

    private suspend fun loginNotSupported(extName: String?) {
        val login = app.context.getString(R.string.login)
        val message =
            app.context.getString(R.string.x_is_not_supported_in_x, login, extName.toString())
        messageFlow.emit(Message(message))
        loadingOver.emit(Unit)
    }

    fun changeFragment(type: FragmentType) = viewModelScope.launch {
        addFragmentFlow.emit(type)
    }

    private suspend fun afterLogin(
        result: Result<List<User>>
    ) {
        val users = result.getOrElse {
            app.throwFlow.emit(it)
            loading.value = false
            loadingOver.emit(Unit)
            return@afterLogin
        }
        if (users.isEmpty()) {
            app.messageFlow.emit(Message(app.context.getString(R.string.no_user_found)))
        } else {
            val entities = users.map { it.toEntity(extensionType, extensionId!!) }
            userDao.insertUsers(entities)
            val user = entities.first()
            userDao.setCurrentUser(user.toCurrentUser())
        }
        loading.value = false
        loadingOver.emit(Unit)
    }

    fun onWebViewStop(
        result: Result<List<User>>,
    ) = viewModelScope.launch {
        val extension = extension.first { it != null }!!
        val users = runCatching { result.getOrElse { throw it.toAppException(extension) } }
        afterLogin(users)
    }

    val inputs = mutableMapOf<String, String?>()
    fun onCustomTextInputSubmit(form: LoginClient.Form) = viewModelScope.launch {
        loading.value = true
        val extension = extension.first { it != null }!!
        val users = extension.getAs<LoginClient.CustomInput, List<User>> {
            onLogin(form.key, inputs.toMap())
        }
        afterLogin(users)
    }

    init {
        viewModelScope.launch {
            val extension = extension.first { it != null }!!
            val totalClients = extension.get {
                listOfNotNull(
                    if (this is LoginClient.WebView) 1 else 0,
                    if (this is LoginClient.CustomInput) forms.size
                    else 0,
                ).sum()
            }.getOrNull() ?: 0
            val client = extension.instance.value().getOrNull()
            when (totalClients) {
                0 -> loginNotSupported(extension.name)
                1 -> when (client) {
                    is LoginClient.WebView -> addFragmentFlow.emit(FragmentType.WebView)
                    is LoginClient.CustomInput -> addFragmentFlow.emit(FragmentType.CustomInput(null))
                    null -> loginNotSupported(extension.name)
                }

                else -> addFragmentFlow.emit(FragmentType.Selector)
            }
        }
    }
}