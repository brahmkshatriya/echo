package dev.brahmkshatriya.echo.viewmodels

import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.newui.exception.openException
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnackBarViewModel @Inject constructor(
    mutableThrowableFlow: MutableSharedFlow<Throwable>
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
        if (dismissed) messages.remove(message)
        if (messages.isNotEmpty()) viewModelScope.launch {
            _messageFlow.emit(messages.first())
        }
    }

    companion object {
        fun AppCompatActivity.configureSnackBar(navController: NavController, root: View) {
            val viewModel by viewModels<SnackBarViewModel>()
            fun createSnackBar(message: Message) {
                val snackBar = Snackbar.make(
                    root,
                    message.message,
                    Snackbar.LENGTH_LONG
                )
                snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
                snackBar.view.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(0) }
                snackBar.anchorView = root

                snackBar.setAction(message.action) {
                    val actionHandler = message.actionHandler
                    if (actionHandler != null) actionHandler(navController)
                }
                snackBar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        viewModel.remove(message, event != DISMISS_EVENT_MANUAL)
                    }
                })
                snackBar.show()
            }

            observe(viewModel.messageFlow) { message ->
                println("SnackBarViewModel: $message")
                createSnackBar(message)
            }

            observe(viewModel.throwableFlow) { throwable ->
                throwable.printStackTrace()
                val message = Message(
                    message = throwable.message ?: "An error occurred",
                    action = getString(R.string.view),
                    actionHandler = { navController ->
                        openException(navController, throwable)
                    }
                )
                viewModel.create(message)
            }
        }

        fun Fragment.createSnack(
            message: String,
            action: String = "",
            actionHandler: ((NavController) -> Unit)? = null
        ) {
            val snack = Message(message, action, actionHandler)
            val viewModel by activityViewModels<SnackBarViewModel>()
            viewModel.create(snack)
        }

        fun Fragment.createSnack(
            messageRes: Int,
            actionRes: Int = 0,
            actionHandler: ((NavController) -> Unit)? = null
        ) {
            val message = getString(messageRes)
            val action = if (actionRes == 0) "" else getString(actionRes)
            createSnack(message, action, actionHandler)
        }
    }
}