package dev.brahmkshatriya.echo.newui.exception

import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController

fun FragmentActivity.openException(navController: NavController, throwable: Throwable) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    val action = ExceptionFragmentDirections.actionException()
    navController.navigate(action)
}