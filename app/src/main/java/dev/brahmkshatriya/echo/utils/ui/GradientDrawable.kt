package dev.brahmkshatriya.echo.utils.ui

import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.image.BlurTransformation.Companion.blurred
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

    private const val RATIO = 0.33f
    private val maxColor = Color.argb(128,0,0,0)
    fun createBlurred(view: View, toBlur: Drawable?): Drawable {
        val background = MaterialColors.getColor(view, R.attr.echoBackground)
        if (toBlur == null) return background.toDrawable()
        val noise = ResourcesCompat.getDrawable(view.resources, R.drawable.grain_noise, view.context.theme)!!.toBitmap()
        val bitmap = toBlur.run {
            toBitmap(
                intrinsicWidth.coerceAtLeast(1),
                intrinsicHeight.coerceAtLeast(1),
            )
        }.blurred(view.context)
        return PaintDrawable().apply {
            setShape(RectShape())
            paint.shader = null
            shaderFactory = object : ShapeDrawable.ShaderFactory() {
                override fun resize(width: Int, height: Int): Shader {
                    val bitmapShader = BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
                    val cropHeight = (height * RATIO).toInt().coerceAtLeast(1)
                    val scale = maxOf(
                        width.toFloat() / bitmap.width,
                        cropHeight.toFloat() / bitmap.height
                    )
                    val matrix = Matrix().apply {
                        setScale(scale, scale)
                    }
                    bitmapShader.setLocalMatrix(matrix)
                    val composed = ComposeShader(
                        bitmapShader,
                        LinearGradient(
                            0f,
                            0f,
                            0f,
                            height.toFloat(),
                            intArrayOf(maxColor, Color.TRANSPARENT, Color.TRANSPARENT),
                            floatArrayOf(0f, RATIO, 1f),
                            Shader.TileMode.CLAMP
                        ),
                        PorterDuff.Mode.DST_IN
                    )
                    val withNoise = ComposeShader(
                        composed,
                        BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT),
                        PorterDuff.Mode.DST_IN
                    )
                    val backgroundShader = LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        intArrayOf(background, background),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    return ComposeShader(
                        backgroundShader,
                        withNoise,
                        PorterDuff.Mode.SRC_OVER
                    )
                }
            }
        }
    }
}