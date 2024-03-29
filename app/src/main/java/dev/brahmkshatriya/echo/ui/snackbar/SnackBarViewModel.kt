package dev.brahmkshatriya.echo.ui.snackbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnackBarViewModel @Inject constructor(
    mutableThrowableFlow : MutableSharedFlow<Throwable>
) : ViewModel() {

    val throwableFlow = mutableThrowableFlow.asSharedFlow()

    data class Message(
        val message: String,
        val action: String,
        val actionHandler: ((NavController) -> Unit)? = null
    )

    private val _messageFlow = MutableSharedFlow<Message>()
    val messageFlow = _messageFlow.asSharedFlow()

    private val messages = mutableListOf<Message>()

    fun create(message: Message) {
        if (messages.isEmpty()) viewModelScope.launch {
            _messageFlow.emit(message)
        }
        messages.add(message)
    }

    fun remove(message: Message, dismissed: Boolean) {
        if(dismissed) messages.remove(message)
        if (messages.isNotEmpty()) viewModelScope.launch {
            _messageFlow.emit(messages.first())
        }
    }
}