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
        val large = RemoteViews(packageName, R.layout.widget_horizontal_large)
        val small = RemoteViews(packageName, R.layout.widget_horizontal_small)

        val views = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            updateView(clazz, controller, image, context, small)
            small
        } else {
            val widgetLayout = mapOf(
                SizeF(0f, 0f) to small,
                SizeF(256f, 128f) to large,
            )
            widgetLayout.forEach { (_, u) ->
                updateView(clazz, controller, image, context, u)
            }
            RemoteViews(widgetLayout)
        }
        return views
    }

}
