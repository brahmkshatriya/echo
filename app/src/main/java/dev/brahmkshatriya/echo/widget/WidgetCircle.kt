package dev.brahmkshatriya.echo.widget

import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.R

class WidgetCircle : BaseWidget() {
    override val clazz = this::class.java
    override fun updatedViews(
        controller: MediaController?, image: Bitmap?, context: Context, appWidgetId: Int
    ): RemoteViews {
        val packageName = context.packageName
        val view = RemoteViews(packageName, R.layout.widget_circle)
        updateView(clazz, controller, image, context, view)
        return view
    }
}