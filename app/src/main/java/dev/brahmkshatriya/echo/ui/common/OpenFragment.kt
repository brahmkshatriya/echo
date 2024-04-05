package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun Fragment.openFragment(newFragment: Fragment, view: View? = null) {
    val transaction = parentFragmentManager.beginTransaction()
    if (view != null)
        transaction.addSharedElement(view, view.transitionName)

    if (this is MainFragment)
        transaction.add(R.id.navHostFragment, newFragment).hide(this)
    else transaction.replace(R.id.navHostFragment, newFragment)

    val uiViewModel by activityViewModels<UiViewModel>()
    uiViewModel.isMainFragment.value = newFragment is MainFragment
    transaction.addToBackStack(null).commit()
}
