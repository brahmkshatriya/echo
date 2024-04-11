package dev.brahmkshatriya.echo.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun Fragment.openFragment(newFragment: Fragment, view: View? = null) {
    parentFragmentManager.commit {
        if (view != null) {
            addSharedElement(view, view.transitionName)
            newFragment.run {
                if(arguments == null) arguments = Bundle()
                arguments!!.putString("transitionName", view.transitionName)
            }
        }
        setReorderingAllowed(true)
        val fragment = this@openFragment
        add(R.id.navHostFragment, newFragment)
        hide(fragment)
        addToBackStack(null)
    }
    val uiViewModel by activityViewModels<UiViewModel>()
    uiViewModel.isMainFragment.value = newFragment is MainFragment
}

fun FragmentActivity.openFragment(newFragment: Fragment, view: View? = null) {
    val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
    oldFragment.openFragment(newFragment, view)
}