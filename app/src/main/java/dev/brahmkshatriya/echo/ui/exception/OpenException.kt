package dev.brahmkshatriya.echo.ui.exception

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.common.exceptions.UnauthorizedException
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.login.LoginFragment
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun FragmentActivity.openException(throwable: Throwable, view: View? = null) {
    openFragment(ExceptionFragment.newInstance(throwable), view)
    val uiViewModel: UiViewModel by viewModels()
    uiViewModel.collapsePlayer()
}


fun FragmentActivity.openLoginException(
    throwable: LoginRequiredException,
    view: View? = null
) {
    if (throwable is UnauthorizedException) {
        val model by viewModels<LoginUserViewModel>()
        model.logout(throwable.clientId, throwable.userId)
    }
    openFragment(LoginFragment.newInstance(throwable), view)
}