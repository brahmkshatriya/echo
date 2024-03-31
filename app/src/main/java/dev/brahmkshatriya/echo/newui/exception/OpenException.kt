package dev.brahmkshatriya.echo.newui.exception

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras

fun FragmentActivity.openException(view: View, throwable: Throwable) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    val transitionName = throwable.hashCode().toString()
    view.transitionName = transitionName
    val extras = FragmentNavigatorExtras(view to transitionName)
    val action = ExceptionFragmentDirections.actionException()
    view.findNavController().navigate(action, extras)
}

fun FragmentActivity.openException(navController: NavController,throwable: Throwable) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    val action = ExceptionFragmentDirections.actionException()
    navController.navigate(action)
}