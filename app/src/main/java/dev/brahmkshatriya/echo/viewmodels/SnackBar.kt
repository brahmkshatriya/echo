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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.getTitle
import dev.brahmkshatriya.echo.ui.exception.openException
import dev.brahmkshatriya.echo.ui.login.LoginFragment
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnackBar @Inject constructor(
    mutableThrowableFlow: MutableSharedFlow<Throwable>,
    val mutableMessageFlow: MutableSharedFlow<Message>
) : ViewModel() {

    val throwableFlow = mutableThrowableFlow.asSharedFlow()

    data class Message(
        val message: String,
        val action: Action? = null
    )

    data class Action(
        val name: String,
        val handler: (() -> Unit)
    )

    private val messages = mutableListOf<Message>()

    fun create(message: Message) {
        if (messages.isEmpty()) viewModelScope.launch {
            mutableMessageFlow.emit(message)
        }
        if (!messages.contains(message)) messages.add(message)
    }

    fun remove(message: Message, dismissed: Boolean) {
        if (dismissed) messages.remove(message)
        if (messages.isNotEmpty()) viewModelScope.launch {
            mutableMessageFlow.emit(messages.first())
        }
    }

    companion object {
        fun AppCompatActivity.configureSnackBar(root: View) {
            val viewModel by viewModels<SnackBar>()
            fun createSnackBar(message: Message) {
                val snackBar = Snackbar.make(
                    root,
                    message.message,
                    Snackbar.LENGTH_LONG
                )
                snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
                snackBar.view.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(0) }
                snackBar.anchorView = root
                message.action?.run { snackBar.setAction(name) { handler() } }
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
                val message = when (throwable) {
                    is LoginRequiredException -> Message(
                        message = getString(R.string.login_required),
                        action = Action(getString(R.string.login)) {
                            openFragment(LoginFragment.newInstance(throwable))
                        }
                    )

                    else -> Message(
                        message = getTitle(throwable),
                        action = Action(getString(R.string.view)) { openException(throwable) }
                    )
                }
                viewModel.create(message)
            }
        }

        fun Fragment.createSnack(message: Message) {
            val viewModel by activityViewModels<SnackBar>()
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