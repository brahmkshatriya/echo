package dev.brahmkshatriya.echo.utils

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.TypedValue
import android.view.View
import com.google.android.material.navigationrail.NavigationRailView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isRTL

fun View.createNavDrawable(bottom: Int = 0) {
    val isRail = this is NavigationRailView
    val isRTL = context.isRTL()
    val centerY = if (isRail) 1f else (height * 0.66f + bottom) / (height + bottom)
    background = PaintDrawable().apply {
        val typed = TypedValue()
        context.theme.resolveAttribute(R.attr.navBackground, typed, true)
        setShape(RectShape())
        shaderFactory = object : ShapeDrawable.ShaderFactory() {
            override fun resize(width: Int, height: Int): Shader {
                val startX = if (isRail) if (isRTL) width.toFloat() else 0f else width / 2f
                val startY = if (isRail) height / 2f else 0f
                val endX = if (isRail) if (isRTL) 0f else width.toFloat() else width / 2f
                val endY = if (isRail) height / 2f else height.toFloat()
                return LinearGradient(
                    startX,
                    startY,
                    endX,
                    endY,
                    intArrayOf(Color.TRANSPARENT, typed.data, typed.data),
                    floatArrayOf(0f, centerY, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }
    }
}