package dev.brahmkshatriya.echo.utils.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.AppBarLayout
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.BACKGROUND_GRADIENT
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

object UiUtils {

    fun Activity.hideSystemUi(hide: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hide) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun AppBarLayout.configureAppBar(block: (offset: Float) -> Unit) {
        val settings = context.getSettings()
        val isGradient = settings.getBoolean(BACKGROUND_GRADIENT, true)
        val extra = if (isGradient) -191 else 0
        addOnOffsetChangedListener { _, verticalOffset ->
            val offset = -verticalOffset / totalScrollRange.toFloat()
            background?.mutate()?.alpha = max(0, extra + (offset * 255).toInt())
            runCatching { block(offset) }
        }
    }

    fun Context.isRTL() =
        resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    fun Context.isLandscape() =
        resources.configuration.orientation == ORIENTATION_LANDSCAPE

    fun Context.isNightMode() =
        resources.configuration.uiMode and UI_MODE_NIGHT_MASK != UI_MODE_NIGHT_NO

    fun Int.dpToPx(context: Context) = (this * context.resources.displayMetrics.density).toInt()

    fun Context.resolveStyledDimension(attr: Int): Int {
        val typed = theme.obtainStyledAttributes(intArrayOf(attr))
        val itemWidth = typed.getDimensionPixelSize(typed.getIndex(0), 0)
        return itemWidth
    }

    fun Long.toTimeString(): String {
        val seconds = (this.toFloat() / 1000).roundToLong()
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds % 60)
        }
    }

    fun TextView.marquee() {
        isSelected = true
        ellipsize = TextUtils.TruncateAt.MARQUEE
        maxLines = 1
        marqueeRepeatLimit = -1
        setHorizontallyScrolling(true)
    }
}