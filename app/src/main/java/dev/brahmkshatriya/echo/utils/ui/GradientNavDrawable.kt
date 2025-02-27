package dev.brahmkshatriya.echo.utils.ui

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isRTL

object GradientNavDrawable {
    fun apply(
        view: View,
        isRail: Boolean,
        bottom: Int = 0,
        full: Boolean = true
    ) {
        val color = MaterialColors.getColor(view, R.attr.navBackground)
        val isRTL = !view.context.isRTL()
        view.background = PaintDrawable().apply {
            setShape(RectShape())
            shaderFactory = object : ShapeDrawable.ShaderFactory() {
                override fun resize(width: Int, height: Int): Shader {
                    val centerY = if (isRail) 1f else (height * 0.5f + bottom) / (height + bottom)
                    val startX = if (isRail) if (isRTL) width.toFloat() else 0f else width / 2f
                    val startY = if (isRail) height / 2f else 0f
                    val endX = if (isRail) if (isRTL) 0f else width.toFloat() else width / 2f
                    val endY = if (isRail) height / 2f else height.toFloat()
                    return LinearGradient(
                        startX,
                        startY,
                        endX,
                        endY,
                        intArrayOf(if (full) color else Color.TRANSPARENT, color, color),
                        floatArrayOf(0f, centerY, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
            }
        }
    }
}