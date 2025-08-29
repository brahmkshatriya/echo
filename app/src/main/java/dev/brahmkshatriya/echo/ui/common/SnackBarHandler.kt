package dev.brahmkshatriya.echo.ui.common

import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.WeakHashMap

class SnackBarHandler(
    val app: App,
) {

    private val messageFlow = app.messageFlow
    private val messages = mutableListOf<Message>()

    suspend fun create(message: Message) {
        if (messages.isEmpty()) messageFlow.emit(message)
        if (!messages.contains(message)) messages.add(message)
    }

    suspend fun remove(message: Message, dismissed: Boolean) {
        if (dismissed) messages.remove(message)
        if (messages.isNotEmpty()) messageFlow.emit(messages.first())
    }

    companion object {
        fun MainActivity.setupSnackBar(
            uiViewModel: UiViewModel, root: View
        ): SnackBarHandler {
            val handler by inject<SnackBarHandler>()
            val padding = 8.dpToPx(this@setupSnackBar)
            val snackBars = WeakHashMap<Int, Snackbar>()
            fun updateInsets(snackBar: Snackbar) {
                snackBar.view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    val insets = uiViewModel.systemInsets.value
                    val snackbarInsets = uiViewModel.getSnackbarInsets()
                    marginStart = insets.start + snackbarInsets.start + padding
                    marginEnd = insets.end + snackbarInsets.end + padding
                    bottomMargin = snackbarInsets.bottom + padding
                }
            }
            fun createSnackBar(message: Message) {
                val snackBar = Snackbar.make(root, message.message, Snackbar.LENGTH_LONG)
                snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
                updateInsets(snackBar)
                message.action?.run { snackBar.setAction(name) { handler() } }
                snackBars[message.hashCode()] = snackBar
                snackBar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        snackBars.remove(message.hashCode())
                        lifecycleScope.launch {
                            handler.remove(message, event != DISMISS_EVENT_MANUAL)
                        }
                    }
                })
                snackBar.show()
            }

            observe(handler.messageFlow) { message ->
                createSnackBar(message)
            }
            observe(uiViewModel.combined) { _ ->
                snackBars.values.forEach { updateInsets(it) }
            }
            return handler
        }

        fun Fragment.createSnack(message: Message) {
            val handler by inject<SnackBarHandler>()
            lifecycleScope.launch { handler.create(message) }
        }

        fun Fragment.createSnack(message: String) {
            createSnack(Message(message))
        }

        fun Fragment.createSnack(message: Int) {
            createSnack(getString(message))
        }

        fun FragmentActivity.createSnack(message: Message) {
            val handler by inject<SnackBarHandler>()
            lifecycleScope.launch { handler.create(message) }
        }

        fun FragmentActivity.createSnack(message: String) {
            createSnack(Message(message))
        }
    }
}