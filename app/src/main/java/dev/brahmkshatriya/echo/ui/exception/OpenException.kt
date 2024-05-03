package dev.brahmkshatriya.echo.ui.exception

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun FragmentActivity.openException(throwable: Throwable, view: View? = null) {
    openFragment(ExceptionFragment.newInstance(throwable), view)
    val uiViewModel: UiViewModel by viewModels()
    uiViewModel.collapsePlayer()
}