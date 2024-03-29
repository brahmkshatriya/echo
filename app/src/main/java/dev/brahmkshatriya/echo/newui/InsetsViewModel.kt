package dev.brahmkshatriya.echo.newui

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class InsetsViewModel : ViewModel() {
    data class Insets(
        val top: Int = 0,
        val bottom: Int = 0,
        val start: Int = 0,
        val end: Int = 0
    ) {
        fun add(vararg insets: Insets) = insets.fold(this) { acc, it ->
            Insets(
                acc.top + it.top,
                acc.bottom + it.bottom,
                acc.start + it.start,
                acc.end + it.end
            )
        }
    }

    private val navView = MutableStateFlow(Insets())
    private val systemInsets = MutableStateFlow(Insets())

    val combined = navView.combine(systemInsets) { nav, sys ->
        nav.add(sys)
    }

    fun setNavInsets(context: Context, isNavVisible: Boolean, isRail: Boolean) {
        context.resources.run {
            val insets = if (isNavVisible) {
                if (isRail) {
                    val width = getDimensionPixelSize(R.dimen.nav_width)
                    if (context.isRTL()) Insets(end = width)
                    else Insets(start = width)
                } else Insets(bottom = getDimensionPixelSize(R.dimen.nav_height))
            } else Insets()
            navView.value = insets
        }
    }

    fun setSystemInsets(context: Context, insets: WindowInsetsCompat) {
        val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val inset = system.run {
            if (context.isRTL()) Insets(top, bottom, right, left)
            else Insets(top, bottom, left, right)
        }
        systemInsets.value = inset
    }

    companion object {
        fun Context.isRTL() =
            resources.configuration.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL

        fun Fragment.applyInsets(appBar: View, child: View, block: InsetsViewModel.(Insets) -> Unit = {}) {
            val insetsViewModel by activityViewModels<InsetsViewModel>()
            observe(insetsViewModel.combined) { insets ->
                val verticalPadding = 8.dpToPx(requireContext())
                child.updatePaddingRelative(
                    top = verticalPadding,
                    bottom = insets.bottom + verticalPadding,
                    start = insets.start,
                    end = insets.end
                )
                appBar.updatePaddingRelative(
                    start = insets.start,
                    end = insets.end
                )
                insetsViewModel.block(insets)
            }
        }
    }
}