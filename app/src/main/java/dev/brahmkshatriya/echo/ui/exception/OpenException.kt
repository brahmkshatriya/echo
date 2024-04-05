package dev.brahmkshatriya.echo.ui.exception

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.openException(view: View, throwable: Throwable) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    supportFragmentManager.beginTransaction()
        .add(android.R.id.content, ExceptionFragment())
        .addSharedElement(view, view.transitionName!!)
        .addToBackStack(null)
        .commit()
}

fun FragmentActivity.openException(throwable: Throwable) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    supportFragmentManager.beginTransaction()
        .add(android.R.id.content, ExceptionFragment())
        .addToBackStack(null)
        .commit()
}