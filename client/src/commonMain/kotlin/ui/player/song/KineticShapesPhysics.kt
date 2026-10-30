package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class KineticShapeContour(polygon: RoundedPolygon) {
    val vertices: FloatArray
    val boundRadius: Float

    init {
        val samples = ArrayList<Float>(polygon.cubics.size * 8)
        polygon.cubics.forEach { cubic ->
            val x0 = cubic.anchor0X
            val y0 = cubic.anchor0Y
            val x1 = cubic.control0X
            val y1 = cubic.control0Y
            val x2 = cubic.control1X
            val y2 = cubic.control1Y
            val x3 = cubic.anchor1X
            val y3 = cubic.anchor1Y
            fun sample(t: Float) {
                val u = 1f - t
                val x = u * u * u * x0 + 3f * u * u * t * x1 + 3f * u * t * t * x2 + t * t * t * x3
                val y = u * u * u * y0 + 3f * u * u * t * y1 + 3f * u * t * t * y2 + t * t * t * y3
                val px = (x - 0.5f) * 2f
                val py = (y - 0.5f) * 2f
                val n = samples.size
                if (
                    n >= 2 &&
                        (px - samples[n - 2]) * (px - samples[n - 2]) +
                            (py - samples[n - 1]) * (py - samples[n - 1]) < 0.00001f
                )
                    return
                samples.add(px)
                samples.add(py)
            }
            for (step in 0..6) sample(step / 6f)
        }
        var hull =
            listOf(
                floatArrayOf(-2f, -2f),
                floatArrayOf(2f, -2f),
                floatArrayOf(2f, 2f),
                floatArrayOf(-2f, 2f),
            )
        val axisCount = 20
        for (axis in 0 until axisCount) {
            val angle = 2f * PI.toFloat() * axis / axisCount
            val nx = cos(angle)
            val ny = sin(angle)
            var support = Float.NEGATIVE_INFINITY
            for (i in samples.indices step 2) support =
                max(support, samples[i] * nx + samples[i + 1] * ny)
            support += 0.016f
            val clipped = ArrayList<FloatArray>(hull.size + 2)
            var previous = hull.last()
            var previousDistance = support - previous[0] * nx - previous[1] * ny
            for (current in hull) {
                val distance = support - current[0] * nx - current[1] * ny
                if ((previousDistance >= 0f) != (distance >= 0f)) {
                    val t = previousDistance / (previousDistance - distance)
                    clipped.add(
                        floatArrayOf(
                            previous[0] + (current[0] - previous[0]) * t,
                            previous[1] + (current[1] - previous[1]) * t,
                        )
                    )
                }
                if (distance >= 0f) clipped.add(current)
                previous = current
                previousDistance = distance
            }
            hull = clipped
        }
        vertices = FloatArray(hull.size * 2) { i -> hull[i / 2][i % 2] }
        var maxDistanceSquared = 0f
        for (i in vertices.indices step 2) maxDistanceSquared =
            max(maxDistanceSquared, vertices[i] * vertices[i] + vertices[i + 1] * vertices[i + 1])
        boundRadius = sqrt(maxDistanceSquared)
    }
}

fun kineticShapePolygons(): List<RoundedPolygon> =
    listOf(
        // Broad, smooth silhouettes rather than dozens of detailed, angular icons.
        // The flatter, organic outlines are closer to the wallpaper reference.
        MaterialShapes.Circle,
        MaterialShapes.Oval,
        MaterialShapes.Pill,
        MaterialShapes.Puffy,
        MaterialShapes.PuffyDiamond,
        MaterialShapes.Bun,
        MaterialShapes.ClamShell,
        MaterialShapes.Ghostish,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.SoftBurst,
    )

enum class KineticShapeLayer {
    PrimarySecondary,
    Tertiary,
}

enum class KineticShapeSize {
    Small,
    Medium,
    Large,
}

class KineticShapeBody(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val shapeIndex: Int,
    val colorIndex: Int,
    val layer: KineticShapeLayer,
    var angle: Float,
    var spin: Float, // radians/second
    val contour: KineticShapeContour,
    val bassSensitivity: Float = 1f,
) {
    var sizeScale = 1f
        private set

    val effectiveRadius
        get() = radius * sizeScale

    val inverseMass = 1f / (radius * radius)
    val inverseInertia
        get() = 2f * inverseMass / (effectiveRadius * effectiveRadius)

    val broadRadius
        get() = effectiveRadius * contour.boundRadius

    val points = FloatArray(contour.vertices.size)
    val normals = FloatArray(contour.vertices.size)
    var minX = 0f
    var maxX = 0f
    var minY = 0f
    var maxY = 0f
    var leftContactX = 0f
    var leftContactY = 0f
    var rightContactX = 0f
    var rightContactY = 0f
    var bottomContactX = 0f
    var bottomContactY = 0f

    fun reactToBass(bass: Float, dt: Float) {
        // Smooth size changes: quick expansion, slower return to the original size.
        val target = 1f + bass * 0.45f * bassSensitivity
        val rate = if (target > sizeScale) 14f else 5f
        val fraction = (1f - exp(-rate * dt)).coerceIn(0f, 1f)
        sizeScale += (target - sizeScale) * fraction
    }

    fun rebuild() {
        val c = cos(angle)
        val s = sin(angle)
        minX = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        val source = contour.vertices
        for (i in source.indices step 2) {
            val px = source[i] * effectiveRadius
            val py = source[i + 1] * effectiveRadius
            val wx = x + px * c - py * s
            val wy = y + px * s + py * c
            points[i] = wx
            points[i + 1] = wy
            if (wx < minX) {
                minX = wx
                leftContactX = wx
                leftContactY = wy
            }
            if (wx > maxX) {
                maxX = wx
                rightContactX = wx
                rightContactY = wy
            }
            if (wy < minY) minY = wy
            if (wy > maxY) {
                maxY = wy
                bottomContactX = wx
                bottomContactY = wy
            }
        }
        var previous = points.size - 2
        for (i in points.indices step 2) {
            val ex = points[i] - points[previous]
            val ey = points[i + 1] - points[previous + 1]
            val lengthSquared = ex * ex + ey * ey
            if (lengthSquared > 0.00001f) {
                val scale = 1f / sqrt(lengthSquared)
                normals[i] = -ey * scale
                normals[i + 1] = ex * scale
            } else {
                normals[i] = 0f
                normals[i + 1] = 0f
            }
            previous = i
        }
    }

    fun translateBy(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) return
        x += dx
        y += dy
        minX += dx
        maxX += dx
        minY += dy
        maxY += dy
        leftContactX += dx
        leftContactY += dy
        rightContactX += dx
        rightContactY += dy
        bottomContactX += dx
        bottomContactY += dy
        for (i in points.indices step 2) {
            points[i] += dx
            points[i + 1] += dy
        }
    }
}

private class ShapeContact(
    var nx: Float = 0f,
    var ny: Float = 0f,
    var depth: Float = 0f,
    var px: Float = 0f,
    var py: Float = 0f,
)

private fun contact(a: KineticShapeBody, b: KineticShapeBody, hit: ShapeContact): Boolean {
    if (a.maxX < b.minX || a.minX > b.maxX || a.maxY < b.minY || a.minY > b.maxY) return false
    val dx = b.x - a.x
    val dy = b.y - a.y
    val radius = a.broadRadius + b.broadRadius
    if (dx * dx + dy * dy > radius * radius) return false

    var bestDepth = Float.POSITIVE_INFINITY
    var bestX = 0f
    var bestY = 0f
    fun axes(body: KineticShapeBody): Boolean {
        val normals = body.normals
        for (i in normals.indices step 2) {
            val nx = normals[i]
            val ny = normals[i + 1]
            if (nx == 0f && ny == 0f) continue
            var minA = Float.POSITIVE_INFINITY
            var maxA = Float.NEGATIVE_INFINITY
            var minB = Float.POSITIVE_INFINITY
            var maxB = Float.NEGATIVE_INFINITY
            for (k in a.points.indices step 2) {
                val dot = a.points[k] * nx + a.points[k + 1] * ny
                if (dot < minA) minA = dot
                if (dot > maxA) maxA = dot
            }
            for (k in b.points.indices step 2) {
                val dot = b.points[k] * nx + b.points[k + 1] * ny
                if (dot < minB) minB = dot
                if (dot > maxB) maxB = dot
            }
            val positive = maxA - minB
            val negative = maxB - minA
            if (positive <= 0f || negative <= 0f) return false
            val depth = min(positive, negative)
            if (depth < bestDepth) {
                bestDepth = depth
                val direction = if (positive < negative) 1f else -1f
                bestX = nx * direction
                bestY = ny * direction
            }
        }
        return true
    }
    if (!axes(a) || !axes(b)) return false

    var aExtreme = Float.NEGATIVE_INFINITY
    var bExtreme = Float.POSITIVE_INFINITY
    var aX = a.x
    var aY = a.y
    var bX = b.x
    var bY = b.y
    for (k in a.points.indices step 2) {
        val dot = a.points[k] * bestX + a.points[k + 1] * bestY
        if (dot > aExtreme) {
            aExtreme = dot
            aX = a.points[k]
            aY = a.points[k + 1]
        }
    }
    for (k in b.points.indices step 2) {
        val dot = b.points[k] * bestX + b.points[k + 1] * bestY
        if (dot < bExtreme) {
            bExtreme = dot
            bX = b.points[k]
            bY = b.points[k + 1]
        }
    }
    hit.nx = bestX
    hit.ny = bestY
    hit.depth = bestDepth
    hit.px = (aX + bX) * 0.5f
    hit.py = (aY + bY) * 0.5f
    return true
}

private fun resolve(a: KineticShapeBody, b: KineticShapeBody, hit: ShapeContact, dp: Float) {
    val sumMass = a.inverseMass + b.inverseMass
    val correction = (hit.depth - 0.03f * dp).coerceAtLeast(0f) * 0.92f / sumMass
    a.translateBy(-hit.nx * correction * a.inverseMass, -hit.ny * correction * a.inverseMass)
    b.translateBy(hit.nx * correction * b.inverseMass, hit.ny * correction * b.inverseMass)

    val ax = hit.px - a.x
    val ay = hit.py - a.y
    val bx = hit.px - b.x
    val by = hit.py - b.y
    val crossA = ax * hit.ny - ay * hit.nx
    val crossB = bx * hit.ny - by * hit.nx
    val rvx = (b.vx - b.spin * by) - (a.vx - a.spin * ay)
    val rvy = (b.vy + b.spin * bx) - (a.vy + a.spin * ax)
    val normalSpeed = rvx * hit.nx + rvy * hit.ny
    if (normalSpeed >= 0f) return
    // In weightlessness even slow collisions should gently rebound.
    val restitution = if (normalSpeed < -0.8f * dp) 0.65f else 0f
    val impulse =
        -(1f + restitution) * normalSpeed /
            (sumMass + crossA * crossA * a.inverseInertia + crossB * crossB * b.inverseInertia)
    a.vx -= hit.nx * impulse * a.inverseMass
    a.vy -= hit.ny * impulse * a.inverseMass
    a.spin -= crossA * impulse * a.inverseInertia
    b.vx += hit.nx * impulse * b.inverseMass
    b.vy += hit.ny * impulse * b.inverseMass
    b.spin += crossB * impulse * b.inverseInertia

    val tx = -hit.ny
    val ty = hit.nx
    val tangentA = ax * ty - ay * tx
    val tangentB = bx * ty - by * tx
    val tangentSpeed =
        ((b.vx - b.spin * by) - (a.vx - a.spin * ay)) * tx +
            ((b.vy + b.spin * bx) - (a.vy + a.spin * ax)) * ty
    val friction =
        (-tangentSpeed /
                (sumMass +
                    tangentA * tangentA * a.inverseInertia +
                    tangentB * tangentB * b.inverseInertia))
            .coerceIn(-impulse * 0.16f, impulse * 0.16f)
    a.vx -= tx * friction * a.inverseMass
    a.vy -= ty * friction * a.inverseMass
    a.spin -= tangentA * friction * a.inverseInertia
    b.vx += tx * friction * b.inverseMass
    b.vy += ty * friction * b.inverseMass
    b.spin += tangentB * friction * b.inverseInertia
}

/** Endless weightless field: the camera climbs while shapes drift and collide. */
private const val ShapeSizeMultiplier = 2f
private const val PrimaryScrollSpeedDp = 19f
private const val TertiaryScrollSpeedMultiplier = 1.6f
private const val MaxVisibleFieldBodies = 12
private const val InitialPrimaryBodies = 4
private const val InitialTertiaryBodies = 3
private const val PrimarySpawnIntervalSeconds = 3.8f

// The faster tertiary layer still spawns 25% less frequently than the first.
private const val TertiarySpawnIntervalSeconds = PrimarySpawnIntervalSeconds / 0.75f

class KineticShapesSimulation(
    var width: Float,
    var height: Float,
    private val dp: Float,
    seed: Int,
    private val contours: List<KineticShapeContour>,
    private val zoom: Float,
) {
    private val random = Random(seed)
    val bodies = ArrayList<KineticShapeBody>(MaxVisibleFieldBodies)
    var cameraOffset = 0f
        private set

    var tertiaryCameraOffset = 0f
        private set

    private val hit = ShapeContact()
    private var elapsed = 0f
    private var primarySpawnTimer = 0f
    private var tertiarySpawnTimer = 0f
    private var nextShape = 0
    private var shapes = contours.indices.shuffled(random)

    // The camera's visible rectangle is not a physical boundary.
    private val visibleLeft
        get() = (width - width / zoom) / 2f

    private val visibleRight
        get() = (width + width / zoom) / 2f

    fun cameraOffsetFor(layer: KineticShapeLayer): Float =
        when (layer) {
            KineticShapeLayer.PrimarySecondary -> cameraOffset
            KineticShapeLayer.Tertiary -> tertiaryCameraOffset
        }

    private fun visibleTop(layer: KineticShapeLayer) =
        (height - height / zoom) / 2f - cameraOffsetFor(layer)

    private fun visibleBottom(layer: KineticShapeLayer) = visibleTop(layer) + height / zoom

    init {
        // Fewer large shapes and generous negative space, like the reference.
        // Preserve the tertiary layer's 25% lower starting population.
        repeat(InitialPrimaryBodies + InitialTertiaryBodies) { attemptIndex ->
            val layer =
                if (attemptIndex < InitialPrimaryBodies) KineticShapeLayer.PrimarySecondary
                else KineticShapeLayer.Tertiary
            val shapeIndex = shapes[nextShape % shapes.size]
            val radius = nextRadius(shapeIndex, layer)
            val bound = radius * contours[shapeIndex].boundRadius
            val worldWidth = visibleRight - visibleLeft
            val top = visibleTop(layer)
            val worldHeight = height / zoom
            if (worldWidth <= 0f || worldHeight <= 0f) return@repeat
            var attemptsRemaining = 35
            while (attemptsRemaining-- > 0) {
                // Include partially offscreen shapes for a continuous, unbounded field.
                val x =
                    visibleLeft - bound * 0.7f + random.nextFloat() * (worldWidth + bound * 1.4f)
                val y = top - bound * 0.7f + random.nextFloat() * (worldHeight + bound * 1.4f)
                if (place(x, y, radius, layer)) break
            }
        }
    }

    fun resize(newWidth: Float, newHeight: Float) {
        if (newWidth <= 0f || newHeight <= 0f || (newWidth == width && newHeight == height)) return
        val dx = (newWidth - width) * 0.5f
        val dy = (newHeight - height) * 0.5f
        width = newWidth
        height = newHeight
        for (body in bodies) body.translateBy(dx, dy)
    }

    private fun nextRadius(shapeIndex: Int, layer: KineticShapeLayer): Float {
        val choice = random.nextFloat()
        val category =
            when (layer) {
                KineticShapeLayer.PrimarySecondary ->
                    when {
                        choice < 0.16f -> KineticShapeSize.Small
                        choice < 0.66f -> KineticShapeSize.Medium
                        else -> KineticShapeSize.Large
                    }

                KineticShapeLayer.Tertiary ->
                    if (choice < 0.42f) KineticShapeSize.Medium else KineticShapeSize.Large
            }
        val raw =
            when (category) {
                KineticShapeSize.Small -> 7f + random.nextFloat() * 8f
                KineticShapeSize.Medium -> 16f + random.nextFloat() * 12f
                KineticShapeSize.Large -> 30f + random.nextFloat() * 20f
            } * dp
        val radius =
            min(raw, (visibleRight - visibleLeft) * 0.34f / contours[shapeIndex].boundRadius) *
                ShapeSizeMultiplier
        return radius
    }

    private fun place(x: Float, y: Float, radius: Float, layer: KineticShapeLayer): Boolean {
        if (bodies.size >= MaxVisibleFieldBodies) return false
        val shapeIndex = shapes[nextShape % shapes.size]
        val bound = radius * contours[shapeIndex].boundRadius
        for (other in bodies) {
            if (other.layer != layer) continue // Different layers are free to overlap.
            val dx = x - other.x
            val dy = y - other.y
            val sum = (bound + other.broadRadius) * 0.92f + 2f * dp
            if (dx * dx + dy * dy < sum * sum) return false
        }
        val body =
            KineticShapeBody(
                x,
                y,
                (random.nextFloat() - 0.5f) * 23f * dp,
                (random.nextFloat() - 0.5f) * 21f * dp,
                radius,
                shapeIndex,
                random.nextInt(if (layer == KineticShapeLayer.PrimarySecondary) 8 else 4),
                layer,
                random.nextFloat() * (2 * PI).toFloat(),
                (random.nextFloat() - 0.5f) * 0.23f,
                contours[shapeIndex],
                0.75f + random.nextFloat() * 0.5f,
            )
        body.rebuild()
        bodies.add(body)
        nextShape++
        if (nextShape % shapes.size == 0) shapes = contours.indices.shuffled(random)
        return true
    }

    private fun spawnAbove(layer: KineticShapeLayer) {
        if (bodies.size >= MaxVisibleFieldBodies) return
        repeat(12) {
            val shapeIndex = shapes[nextShape % shapes.size]
            val radius = nextRadius(shapeIndex, layer)
            val bound = radius * contours[shapeIndex].boundRadius
            val worldWidth = visibleRight - visibleLeft
            if (worldWidth <= 0f) return
            val x = visibleLeft - bound * 0.7f + random.nextFloat() * (worldWidth + bound * 1.4f)
            // Let the leading edge of a shape enter the view instead of
            // waiting for its entire bounding circle to cross the top.
            val y = visibleTop(layer) - bound * (0.65f + random.nextFloat() * 0.5f)
            if (place(x, y, radius, layer)) return
        }
    }

    fun step(dt: Float, bassLevel: Float = 0f) {
        if (dt <= 0f) return
        // Compress the wide range of FFT bass amplitudes into visible, bounded growth.
        val bass = sqrt((bassLevel.coerceIn(0f, 1f) / 0.25f).coerceIn(0f, 1f))
        for (body in bodies) body.reactToBass(bass, dt)
        elapsed += dt
        cameraOffset += PrimaryScrollSpeedDp * dp * dt
        tertiaryCameraOffset += PrimaryScrollSpeedDp * TertiaryScrollSpeedMultiplier * dp * dt
        primarySpawnTimer += dt
        tertiarySpawnTimer += dt
        if (primarySpawnTimer >= PrimarySpawnIntervalSeconds) {
            primarySpawnTimer -= PrimarySpawnIntervalSeconds
            spawnAbove(KineticShapeLayer.PrimarySecondary)
        }
        if (tertiarySpawnTimer >= TertiarySpawnIntervalSeconds) {
            tertiarySpawnTimer -= TertiarySpawnIntervalSeconds
            spawnAbove(KineticShapeLayer.Tertiary)
        }
        val h = dt / 3f
        repeat(3) {
            for (body in bodies) {
                val phase = body.shapeIndex * 0.89f + body.radius * 0.012f
                body.vx += sin(elapsed * 0.41f + phase) * 1.4f * dp * h
                body.vy += cos(elapsed * 0.34f + phase) * 1.2f * dp * h
                body.vx *= (1f - 0.18f * h)
                body.vy *= (1f - 0.18f * h)
                val speed = sqrt(body.vx * body.vx + body.vy * body.vy)
                val maximum = 48f * dp
                if (speed > maximum) {
                    body.vx *= maximum / speed
                    body.vy *= maximum / speed
                }
                body.x += body.vx * h
                body.y += body.vy * h
                body.angle += body.spin * h
                body.spin *= (1f - 0.12f * h)
                body.rebuild()
            }
            for ((i, element) in bodies.withIndex()) {
                for (j in i + 1 until bodies.size) {
                    val a = element
                    val b = bodies[j]
                    if (a.layer != b.layer) continue
                    if (contact(a, b, hit)) resolve(a, b, hit, dp)
                }
            }
        }
        // The upward camera eventually leaves the oldest shapes below the view.
        bodies.removeAll {
            it.minY > visibleBottom(it.layer) + 80f * dp ||
                it.maxY < visibleTop(it.layer) - height / zoom * 0.7f
        }
    }
}
