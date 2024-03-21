package dev.brahmkshatriya.echo.ui.snackbar

import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.viewModels
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.observe

fun initSnackBar(activity: MainActivity) {
    val snackBarViewModel: SnackBarViewModel by activity.viewModels()

    activity.apply {
        val navController = binding.navHostFragment.getFragment<NavHostFragment>().navController

        fun createSnackBar(message: SnackBarViewModel.Message) {
            val snackBar = Snackbar.make(
                binding.root,
                message.message,
                Snackbar.LENGTH_LONG
            )
            snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
            snackBar.view.updateLayoutParams<MarginLayoutParams> { setMargins(0) }
            snackBar.anchorView = binding.snackbarContainer

            snackBar.setAction(message.action) {
                val actionHandler = message.actionHandler
                if (actionHandler != null) actionHandler(navController)
            }
            snackBar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    snackBarViewModel.remove(message, event != DISMISS_EVENT_MANUAL)
                }
            })
            snackBar.show()
        }

        observe(snackBarViewModel.messageFlow) { message ->
            createSnackBar(message)
        }

        observe(snackBarViewModel.throwableFlow) { throwable ->
            throwable.printStackTrace()
            val message = SnackBarViewModel.Message(
                message = throwable.message ?: "An error occurred",
                action = getString(R.string.view),
                actionHandler = { navController ->
                    openException(navController, throwable)
                }
            )
            snackBarViewModel.create(message)
        }
    }
}

fun FragmentActivity.openException(navController: NavController, throwable: Throwable) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    val action = ExceptionFragmentDirections.actionException()
    navController.navigate(action)
}