package dev.brahmkshatriya.echo.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.R

class WidgetHorizontal : BaseWidget() {

    override val clazz: Class<*> = this::class.java
    override fun updatedViews(
        controller: MediaController?, image: Bitmap?, context: Context, appWidgetId: Int
    ): RemoteViews {
        val packageName = context.packageName
        val narrowShort = RemoteViews(packageName, R.layout.widget_horizontal_narrow_short)
        val narrowTall = RemoteViews(packageName, R.layout.widget_horizontal_narrow_tall)
        val wideShort = RemoteViews(packageName, R.layout.widget_horizontal_wide_short)
        val wideTall = RemoteViews(packageName, R.layout.widget_horizontal_wide_tall)

        val views = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            updateView(clazz, controller, image, context, narrowShort)
            narrowShort
        } else {
            val widgetLayout = mapOf(
                SizeF(0f, 0f) to narrowShort,
                SizeF(0f, 128f) to narrowTall,
                SizeF(360f, 0f) to wideShort,
                SizeF(360f, 128f) to wideTall,
            )
            widgetLayout.forEach { (_, u) ->
                updateView(clazz, controller, image, context, u)
            }
            RemoteViews(widgetLayout)
        }
        return views
    }

}
