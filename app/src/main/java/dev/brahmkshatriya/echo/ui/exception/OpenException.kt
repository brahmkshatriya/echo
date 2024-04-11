package dev.brahmkshatriya.echo.ui.exception

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun FragmentActivity.openException(throwable: Throwable, view: View? = null) {
    val viewModel: ExceptionFragment.ThrowableViewModel by viewModels()
    viewModel.throwable = throwable
    val uiViewModel: UiViewModel by viewModels()
    uiViewModel.collapsePlayer()
    openFragment(ExceptionFragment(), view)
}