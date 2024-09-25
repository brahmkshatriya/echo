package dev.brahmkshatriya.echo.ui.exception

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.login.LoginFragment
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun FragmentActivity.openException(throwable: Throwable, view: View? = null) {
    openFragment(ExceptionFragment.newInstance(this, throwable), view)
    val uiViewModel: UiViewModel by viewModels()
    uiViewModel.collapsePlayer()
}


fun FragmentActivity.openLoginException(
    throwable: AppException.LoginRequired,
    view: View? = null
) {
    if (throwable is AppException.Unauthorized) {
        val model by viewModels<LoginUserViewModel>()
        model.logout(throwable.extension.id, throwable.userId)
    }
    openFragment(LoginFragment.newInstance(throwable), view)
}