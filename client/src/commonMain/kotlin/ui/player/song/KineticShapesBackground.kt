package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import dev.brahmkshatriya.echo.app.ui.player.artwork.LocalPlayerFavouriteAccent
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

private const val ShapeZoom = 2.5f
// Decorative wallpaper can keep soft tonal relationships without text-level contrast.
private const val ShapeMinimumContrast = 1.9f

private data class VisibleKineticShape(val body: KineticShapeBody, val path: Path)

/** A transformed silhouette in world coordinates, before camera movement and zoom. */
private fun KineticShapeBody.worldPath(normalizedPath: Path): Path = Path().apply {
    addPath(normalizedPath)
    transform(Matrix().apply {
        translate(x, y)
        rotateZ(angle * (180f / PI.toFloat()))
        val diameter = effectiveRadius * 2f
        scale(diameter, diameter)
        translate(-0.5f, -0.5f)
    })
}

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

private fun Color.favouriteTones(background: Color): List<Color> {
    val lab = convert(ColorSpaces.Oklab)
    return listOf(-0.18f, -0.09f, 0f, 0.09f, 0.18f).map { adjustment ->
        Color(
            (lab.red + adjustment).coerceIn(0f, 1f), lab.green, lab.blue, 1f,
            colorSpace = ColorSpaces.Oklab,
        ).convert(ColorSpaces.Srgb).visibleOnBackground(background)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun KineticShapesBackground(
    artworkKey: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = colorScheme
    val primarySecondaryColors = listOf(
        scheme.primary, scheme.primaryFixed, scheme.primaryFixedDim, scheme.primaryContainer,
        scheme.secondary, scheme.secondaryFixed, scheme.secondaryFixedDim, scheme.secondaryContainer,
    ).map { it.visibleOnBackground(scheme.primaryContainer) }
    val tertiaryColors = listOf(
        scheme.tertiary, scheme.tertiaryFixed, scheme.tertiaryFixedDim, scheme.tertiaryContainer,
    ).map { it.visibleOnBackground(scheme.primaryContainer) }
    val favouriteColors = LocalPlayerFavouriteAccent.current.favouriteTones(scheme.primaryContainer)
    val polygons = remember { kineticShapePolygons() }
    val contours = remember(polygons) { polygons.map(::KineticShapeContour) }
    val paths = polygons.map { it.toPath() }
    val density = LocalDensity.current.density
    val bassLevel = LocalPlayerControls.current.bassLevel
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (constraints.maxWidth <= 0 || constraints.maxHeight <= 0) {
            Canvas(Modifier.fillMaxSize()) { drawRect(scheme.primaryContainer) }
            return@BoxWithConstraints
        }
        // Construct the world during composition, not during measure/layout.
        // Native Compose may process a layout callback on a separate thread;
        // writing snapshot state there can dispatch work on the wrong thread.
        val world = remember(artworkKey, density, contours) {
            KineticShapesSimulation(
                constraints.maxWidth.toFloat().coerceAtLeast(1f),
                constraints.maxHeight.toFloat().coerceAtLeast(1f),
                density, artworkKey.hashCode(), contours, zoom = ShapeZoom,
            )
        }
        SideEffect {
            world.resize(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        }
        var frame by remember(world) { mutableIntStateOf(0) }

    LaunchedEffect(world, active, bassLevel) {
        if (!active) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (coroutineContext.isActive) {
            val now = withFrameNanos { it }
            val delta = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            lastFrame = now
            if (delta > 0f) {
                world.step(delta, bassLevel.value)
                frame++
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        frame
        drawRect(scheme.primaryContainer)
        val visibleHalfWidth = size.width / (2f * ShapeZoom)
        val visibleHalfHeight = size.height / (2f * ShapeZoom)
        val visibleLeft = size.width / 2f - visibleHalfWidth
        val visibleRight = size.width / 2f + visibleHalfWidth
        val tertiaryShift = world.tertiaryCameraOffset - world.cameraOffset
        val baseShapes = ArrayList<VisibleKineticShape>()
        val tertiaryShapes = ArrayList<VisibleKineticShape>()
        for (body in world.bodies) {
            val offset = world.cameraOffsetFor(body.layer)
            val visibleTop = size.height / 2f - visibleHalfHeight - offset
            val visibleBottom = size.height / 2f + visibleHalfHeight - offset
            if (body.maxX < visibleLeft || body.minX > visibleRight ||
                body.maxY < visibleTop || body.minY > visibleBottom) continue
            val path = body.worldPath(paths[body.shapeIndex])
            // Put both silhouettes in the primary layer's coordinate space so
            // their intersections use their actual on-screen positions.
            if (body.layer == KineticShapeLayer.Tertiary) {
                path.translate(Offset(0f, tertiaryShift))
            }
            val shape = VisibleKineticShape(body, path)
            if (body.layer == KineticShapeLayer.PrimarySecondary) baseShapes.add(shape)
            else tertiaryShapes.add(shape)
        }
        scale(scaleX = ShapeZoom, scaleY = ShapeZoom, pivot = size.center) {
            translate(top = world.cameraOffset) {
                // First: all sizes in the primary/secondary color families.
                for ((body, path) in baseShapes) {
                    drawPath(path, primarySecondaryColors[body.colorIndex])
                }
                // Second: medium and large shapes in the tertiary color family.
                for ((body, path) in tertiaryShapes) {
                    drawPath(path, tertiaryColors[body.colorIndex])
                }
                // Recolor the exact silhouette intersections with favorite accent tones.
                for ((a, path) in baseShapes) for ((b, path1) in tertiaryShapes) {
                    if (a.maxX < b.minX || b.maxX < a.minX ||
                        a.maxY < b.minY + tertiaryShift ||
                        b.maxY + tertiaryShift < a.minY) continue
                    val overlap = Path()
                    if (overlap.op(path, path1, PathOperation.Intersect)) {
                        drawPath(overlap, favouriteColors[(a.colorIndex + b.colorIndex) % favouriteColors.size])
                    }
                }
            }
        }
    }
    }
}
