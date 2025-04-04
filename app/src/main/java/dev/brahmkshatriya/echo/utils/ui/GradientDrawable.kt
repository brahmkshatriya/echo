package dev.brahmkshatriya.echo.utils.ui

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isRTL

object GradientDrawable {
    fun applyNav(
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

    const val BACKGROUND_GRADIENT = "bg_gradient"
    private const val RATIO = 0.33f
    private const val INVERTED = 1f - RATIO
    fun createBg(view: View, color: Int): Drawable {
        val echoBackgroundColor = MaterialColors.getColor(view, R.attr.echoBackground)
        val primary = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary)
        val harmonized = MaterialColors.harmonize(color, primary)
        return PaintDrawable().apply {
            setShape(RectShape())
            shaderFactory = object : ShapeDrawable.ShaderFactory() {
                override fun resize(width: Int, height: Int): Shader {
                    fun mix(color: (Int) -> Int): Int {
                        val mixed =
                            RATIO * color(harmonized) + INVERTED * color(echoBackgroundColor)
                        return mixed.toInt()
                    }

                    val mixedColor = Color.argb(
                        255,
                        mix { Color.red(it) },
                        mix { Color.green(it) },
                        mix { Color.blue(it) },
                    )
                    return LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(mixedColor, echoBackgroundColor, echoBackgroundColor),
                        floatArrayOf(0f, 0.33f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
            }
        }
    }
}