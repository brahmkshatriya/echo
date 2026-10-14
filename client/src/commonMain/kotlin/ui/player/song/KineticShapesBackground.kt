package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

private const val ShapeZoom = 2.5f
private const val ShapeMinimumContrast = 3f

private fun Color.contrastOn(background: Color): Float {
    val foregroundLuminance = luminance()
    val backgroundLuminance = background.luminance()
    return (max(foregroundLuminance, backgroundLuminance) + 0.05f) /
        (min(foregroundLuminance, backgroundLuminance) + 0.05f)
}

private fun Color.visibleOnBackground(background: Color): Color {
    if (contrastOn(background) >= ShapeMinimumContrast) return this

    val original = convert(ColorSpaces.Oklab)
    val preferLight = Color.White.contrastOn(background) > Color.Black.contrastOn(background)
    val extremeTone = if (preferLight) 1f else 0f
    fun withTone(tone: Float) = Color(
        tone, original.green, original.blue, original.alpha,
        colorSpace = ColorSpaces.Oklab,
    ).convert(ColorSpaces.Srgb)

    val extreme = withTone(extremeTone)
    if (extreme.contrastOn(background) < ShapeMinimumContrast) {
        return if (preferLight) Color.White else Color.Black
    }

    var tooClose = original.red
    var sufficient = extremeTone
    repeat(10) {
        val midpoint = (tooClose + sufficient) / 2f
        if (withTone(midpoint).contrastOn(background) >= ShapeMinimumContrast) {
            sufficient = midpoint
        } else {
            tooClose = midpoint
        }
    }
    return withTone(sufficient)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun KineticShapesBackground(
    artworkKey: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = colorScheme
    val shades = listOf(
        scheme.primary, scheme.secondary, scheme.tertiary,
        scheme.primaryFixedDim, scheme.secondaryFixedDim, scheme.tertiaryFixedDim,
        scheme.inversePrimary, scheme.tertiaryFixed, LocalPlayerFavouriteAccent.current,
    ).map { it.visibleOnBackground(scheme.primaryContainer) }
    val polygons = remember { kineticShapePolygons() }
    val contours = remember(polygons) { polygons.map(::KineticShapeContour) }
    val paths = polygons.map { it.toPath() }
    val density = LocalDensity.current.density
    val worldState = remember(artworkKey, density, contours) {
        mutableStateOf<KineticShapesSimulation?>(null)
    }
    val world = worldState.value
    var frame by remember(world) { mutableIntStateOf(0) }

    LaunchedEffect(world, active) {
        if (!active || world == null) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (coroutineContext.isActive) {
            val now = withFrameNanos { it }
            val delta = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            lastFrame = now
            if (delta > 0f) {
                world.step(delta)
                frame++
            }
        }
    }

    Canvas(modifier.fillMaxSize().onSizeChanged { viewport ->
        if (viewport.width > 0 && viewport.height > 0) {
            val existing = worldState.value
            if (existing == null) {
                worldState.value = KineticShapesSimulation(
                    viewport.width.toFloat(), viewport.height.toFloat(), density,
                    artworkKey.hashCode(), contours,
                )
            } else {
                existing.resize(viewport.width.toFloat(), viewport.height.toFloat())
            }
        }
    }) {
        frame
        drawRect(scheme.primaryContainer)
        if (world == null) return@Canvas
        val visibleHalfWidth = size.width / (2f * ShapeZoom)
        val visibleHalfHeight = size.height / (2f * ShapeZoom)
        val visibleLeft = size.center.x - visibleHalfWidth
        val visibleRight = size.center.x + visibleHalfWidth
        val visibleTop = size.center.y - visibleHalfHeight
        val visibleBottom = size.center.y + visibleHalfHeight
        scale(scaleX = ShapeZoom, scaleY = ShapeZoom, pivot = size.center) {
            val wall = scheme.outlineVariant.copy(alpha = 0.44f)
            val wallThickness = 1.5.dp.toPx()
            drawLine(wall, Offset(world.left, 0f), Offset(world.left, size.height), wallThickness)
            drawLine(wall, Offset(world.right, 0f), Offset(world.right, size.height), wallThickness)
            if (!world.gateOpen) {
                drawLine(wall, Offset(world.left, world.floor), Offset(world.right, world.floor), wallThickness)
            }
            clipRect(left = world.left, right = world.right) {
                for (body in world.bodies) {
                    if (body.maxX < visibleLeft || body.minX > visibleRight ||
                        body.maxY < visibleTop || body.minY > visibleBottom) continue
                    val diameter = body.radius * 2f
                    translate(body.x - body.radius, body.y - body.radius) {
                        rotate(body.angle * (180f / PI.toFloat()), pivot = Offset(body.radius, body.radius)) {
                            scale(diameter, diameter, pivot = Offset.Zero) {
                                drawPath(paths[body.shapeIndex], shades[body.colorIndex])
                            }
                        }
                    }
                }
            }
        }
    }
}
