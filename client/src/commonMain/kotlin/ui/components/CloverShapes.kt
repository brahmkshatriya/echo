package dev.brahmkshatriya.echo.app.ui.components

import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A six-leaf counterpart to MaterialShapes.Clover4Leaf. */
private val clover6SidedPolygon: RoundedPolygon by lazy {
    val center = 0.5f
    val vertices = FloatArray(6 * 3 * 2)
    val roundings = ArrayList<CornerRounding>(6 * 3)
    val lobeRounding = CornerRounding(0.40f)

    fun addPoint(index: Int, angleDegrees: Float, radius: Float, rounding: CornerRounding) {
        val angle = angleDegrees / 180f * PI.toFloat()
        vertices[index * 2] = center + cos(angle) * radius
        vertices[index * 2 + 1] = center + sin(angle) * radius
        roundings.add(rounding)
    }

    repeat(6) { leaf ->
        val angle = -90f + leaf * 60f
        // Each leaf has a concave notch and two mirrored, rounded outer corners.
        addPoint(leaf * 3, angle, 0.525f, CornerRounding.Unrounded)
        addPoint(leaf * 3 + 1, angle + 15f, 0.66f, lobeRounding)
        addPoint(leaf * 3 + 2, angle + 45f, 0.66f, lobeRounding)
    }

    RoundedPolygon(
        vertices = vertices,
        perVertexRounding = roundings,
        centerX = center,
        centerY = center,
    ).normalized()
}

fun clover6Sided(): RoundedPolygon = clover6SidedPolygon
