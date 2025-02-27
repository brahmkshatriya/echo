package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import dev.brahmkshatriya.echo.R

object FragmentUtils {
    fun Fragment.openFragment(new: Fragment, view: View? = null) {
        parentFragmentManager.commit {
            replace(id, new)
            addToBackStack(null)
        }
    }

    fun FragmentActivity.openFragment(new: Fragment, view: View? = null) {
        val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
        oldFragment.openFragment(new)
    }
}