package dev.brahmkshatriya.echo.viewmodels

import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.exception.openException
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnackBarViewModel @Inject constructor(
    mutableThrowableFlow: MutableSharedFlow<Throwable>,
    val mutableMessageFlow: MutableSharedFlow<Message>
) : ViewModel() {

    val throwableFlow = mutableThrowableFlow.asSharedFlow()

    data class Message(
        val message: String,
        val action: String = "",
        val actionHandler: ((FragmentManager) -> Unit)? = null
    )

    private val messages = mutableListOf<Message>()

    fun create(message: Message) {
        if (messages.isEmpty()) viewModelScope.launch {
            mutableMessageFlow.emit(message)
        }
        messages.add(message)
    }

    fun remove(message: Message, dismissed: Boolean) {
        if (dismissed) messages.remove(message)
        if (messages.isNotEmpty()) viewModelScope.launch {
            mutableMessageFlow.emit(messages.first())
        }
    }

    companion object {
        fun AppCompatActivity.configureSnackBar(fragmentManager: FragmentManager, root: View) {
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
                    if (actionHandler != null) actionHandler(fragmentManager)
                }
                snackBar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        viewModel.remove(message, event != DISMISS_EVENT_MANUAL)
                    }
                })
                snackBar.show()
            }

            observe(viewModel.mutableMessageFlow) { message ->
                createSnackBar(message)
            }

            observe(viewModel.throwableFlow) { throwable ->
                throwable.printStackTrace()
                val message = Message(
                    message = throwable.message ?: "An error occurred",
                    action = getString(R.string.view),
                    actionHandler = {
                        openException(throwable)
                    }
                )
                viewModel.create(message)
            }
        }

        fun Fragment.createSnack(message: Message) {
            val viewModel by activityViewModels<SnackBarViewModel>()
            viewModel.create(message)
        }

        fun Fragment.createSnack(message: String) {
            createSnack(Message(message))
        }

        fun Fragment.createSnack(message: Int) {
            createSnack(getString(message))
        }
    }
}