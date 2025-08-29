@file:Suppress("DEPRECATION")

package dev.brahmkshatriya.echo.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation

class BlurTransformation(
    private val context: Context,
    private val radius: Float = DEFAULT_RADIUS
) : Transformation() {

    override val cacheKey = "${BlurTransformation::class.simpleName}-$radius"

    override suspend fun transform(input: Bitmap, size: Size) =
        input.blurred(context, radius)

    companion object {
        private const val DEFAULT_RADIUS = 20f
        private const val MAX_WIDTH = 256
        fun Bitmap.blurred(
            context: Context,
            radius: Float = DEFAULT_RADIUS
        ): Bitmap {
            require(radius in 0.0..25.0) { "radius must be in [0, 25]." }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val scale = width.toFloat() / MAX_WIDTH
            val scaledHeight = (height / scale).toInt()
            val output =
                createBitmap(MAX_WIDTH, scaledHeight, config ?: Bitmap.Config.ARGB_8888)
            output.applyCanvas {
                scale(1 / scale, 1 / scale)
                drawBitmap(this@blurred, 0f, 0f, paint)
            }

            var script: RenderScript? = null
            var tmpInt: Allocation? = null
            var tmpOut: Allocation? = null
            var blur: ScriptIntrinsicBlur? = null
            try {
                script = RenderScript.create(context)
                tmpInt = Allocation.createFromBitmap(
                    script,
                    output,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT
                )
                tmpOut = Allocation.createTyped(script, tmpInt.type)
                blur = ScriptIntrinsicBlur.create(script, Element.U8_4(script))
                blur.setRadius(radius)
                blur.setInput(tmpInt)
                blur.forEach(tmpOut)
                tmpOut.copyTo(output)
            } finally {
                script?.destroy()
                tmpInt?.destroy()
                tmpOut?.destroy()
                blur?.destroy()
            }

            return output
        }
    }
}