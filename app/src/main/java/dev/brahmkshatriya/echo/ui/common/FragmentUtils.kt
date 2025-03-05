package dev.brahmkshatriya.echo.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import dev.brahmkshatriya.echo.R

object FragmentUtils {
    inline fun <reified T : Fragment> Fragment.openFragment(
        view: View? = null, bundle: Bundle? = null,
    ) {
        parentFragmentManager.commit {
            hide(this@openFragment)
            add<T>(id, args = bundle)
            addToBackStack(null)
        }
    }

    inline fun <reified T : Fragment> FragmentActivity.openFragment(
        view: View? = null, bundle: Bundle? = null
    ) {
        val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
        oldFragment.openFragment<T>(view, bundle)
    }

    inline fun <reified F : Fragment> Fragment.addIfNull(
        id: Int, tag: String, args: Bundle? = null
    ) {
        childFragmentManager.run {
            if (findFragmentByTag(tag) == null) commit {
                add<F>(id, tag, args)
            }
        }
    }
}