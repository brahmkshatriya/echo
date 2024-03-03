package dev.brahmkshatriya.echo.ui.snackbar

import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.viewModels
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.utils.observe

fun initSnackBar(activity: MainActivity) {
    val snackBarViewModel: SnackBarViewModel by activity.viewModels()
    val extensionViewModel: ExtensionViewModel by activity.viewModels()
    activity.apply {
        val navController = binding.navHostFragment.getFragment<NavHostFragment>().navController

        fun createSnackBar(message: SnackBarViewModel.Message) {
            val snackBar = Snackbar.make(
                binding.root,
                message.message,
                Snackbar.LENGTH_INDEFINITE
            )
            snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
            snackBar.view.updateLayoutParams<MarginLayoutParams> { setMargins(0) }
            snackBar.setAction(message.action) {
                val actionHandler = message.actionHandler
                if (actionHandler != null) actionHandler(snackBar.view, navController)
            }
            snackBar.anchorView = binding.bottomPlayerContainer
            snackBar.show()
            snackBar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    snackBarViewModel.remove(message,event == DISMISS_EVENT_ACTION)
                }
            })
        }

        observe(snackBarViewModel.messageFlow) { message ->
            createSnackBar(message)
        }

        observe(snackBarViewModel.exceptionFlow) { exception ->
            val message = SnackBarViewModel.Message(
                message = exception.message ?: "An error occurred",
                action = getString(R.string.view),
                actionHandler = { view, navController ->
                    val action = ExceptionFragmentDirections.actionException(exception)
                    view.transitionName = "exception"
                    val extras = FragmentNavigatorExtras(view to "exception")
                    navController.navigate(action, extras)
                }
            )
            snackBarViewModel.create(message)
        }

        observe(extensionViewModel.exceptionFlow) { e ->
            snackBarViewModel.submitException(e)
        }
    }
}