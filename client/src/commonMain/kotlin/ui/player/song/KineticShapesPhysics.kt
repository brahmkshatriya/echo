package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val CycleSeconds = 10f
private const val OpenAt = 7.2f
private const val CloseAt = 9.5f
private const val TargetCount = 24
private const val MaxCount = 30

internal class KineticShapeContour(polygon: RoundedPolygon) {
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
                val x = u * u * u * x0 + 3f * u * u * t * x1 +
                        3f * u * t * t * x2 + t * t * t * x3
                val y = u * u * u * y0 + 3f * u * u * t * y1 +
                        3f * u * t * t * y2 + t * t * t * y3
                val px = (x - 0.5f) * 2f
                val py = (y - 0.5f) * 2f
                val n = samples.size
                if (n >= 2 && (px - samples[n - 2]) * (px - samples[n - 2]) +
                    (py - samples[n - 1]) * (py - samples[n - 1]) < 0.00001f
                ) return
                samples.add(px)
                samples.add(py)
            }
            for (step in 0..6) sample(step / 6f)
        }
        var hull = listOf(
            floatArrayOf(-2f, -2f), floatArrayOf(2f, -2f),
            floatArrayOf(2f, 2f), floatArrayOf(-2f, 2f),
        )
        val axisCount = 20
        for (axis in 0 until axisCount) {
            val angle = 2f * PI.toFloat() * axis / axisCount
            val nx = cos(angle)
            val ny = sin(angle)
            var support = Float.NEGATIVE_INFINITY
            for (i in samples.indices step 2) {
                support = max(support, samples[i] * nx + samples[i + 1] * ny)
            }
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
        for (i in vertices.indices step 2) {
            maxDistanceSquared = max(
                maxDistanceSquared,
                vertices[i] * vertices[i] + vertices[i + 1] * vertices[i + 1]
            )
        }
        boundRadius = sqrt(maxDistanceSquared)
    }
}

internal fun kineticShapePolygons(): List<RoundedPolygon> = listOf(
    MaterialShapes.Circle, MaterialShapes.Square, MaterialShapes.Slanted,
    MaterialShapes.Arch, MaterialShapes.Fan, MaterialShapes.Arrow,
    MaterialShapes.SemiCircle, MaterialShapes.Oval, MaterialShapes.Pill,
    MaterialShapes.Triangle, MaterialShapes.Diamond, MaterialShapes.ClamShell,
    MaterialShapes.Pentagon, MaterialShapes.Gem, MaterialShapes.Sunny,
    MaterialShapes.VerySunny, MaterialShapes.Cookie4Sided, MaterialShapes.Cookie6Sided,
    MaterialShapes.Cookie7Sided, MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided,
    MaterialShapes.Ghostish, MaterialShapes.Clover4Leaf, MaterialShapes.Clover8Leaf,
    MaterialShapes.Burst, MaterialShapes.SoftBurst, MaterialShapes.Boom,
    MaterialShapes.SoftBoom, MaterialShapes.Flower, MaterialShapes.Puffy,
    MaterialShapes.PuffyDiamond, MaterialShapes.PixelCircle,
    MaterialShapes.PixelTriangle, MaterialShapes.Bun, MaterialShapes.Heart,
)

private enum class ShapeSize { Small, Medium, Huge }

private val sizeCycle = arrayOf(
    ShapeSize.Small, ShapeSize.Huge, ShapeSize.Small,
    ShapeSize.Medium, ShapeSize.Huge, ShapeSize.Small,
)

private data class BodySpec(val shapeIndex: Int, val radius: Float, val bound: Float)

internal class KineticShapeBody(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val shapeIndex: Int,
    val colorIndex: Int,
    var angle: Float,
    var spin: Float, // radians/second
    val contour: KineticShapeContour,
) {
    val inverseMass = 1f / (radius * radius)
    val inverseInertia = 2f * inverseMass / (radius * radius)
    val broadRadius = radius * contour.boundRadius
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

    fun rebuild() {
        val c = cos(angle)
        val s = sin(angle)
        minX = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        val source = contour.vertices
        for (i in source.indices step 2) {
            val px = source[i] * radius
            val py = source[i + 1] * radius
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

private fun wallContact(
    body: KineticShapeBody,
    nx: Float,
    ny: Float,
    depth: Float,
    px: Float,
    py: Float,
    dp: Float
) {
    if (depth <= 0f) return
    body.translateBy(nx * depth, ny * depth)
    val rx = px - body.x
    val ry = py - body.y
    val cross = rx * ny - ry * nx
    val velocityN = (body.vx - body.spin * ry) * nx + (body.vy + body.spin * rx) * ny
    if (velocityN >= 0f) return
    val restitution = if (velocityN < -85f * dp) 0.46f else 0f
    val normalImpulse = -(1f + restitution) * velocityN /
            (body.inverseMass + cross * cross * body.inverseInertia)
    body.vx += nx * normalImpulse * body.inverseMass
    body.vy += ny * normalImpulse * body.inverseMass
    body.spin += cross * normalImpulse * body.inverseInertia
    val tangentialCross = rx * nx - ry * (-ny)
    val speedT = (body.vx - body.spin * ry) * (-ny) + (body.vy + body.spin * rx) * nx
    val friction =
        (-speedT / (body.inverseMass + tangentialCross * tangentialCross * body.inverseInertia))
            .coerceIn(-normalImpulse * 0.65f, normalImpulse * 0.65f)
    body.vx += (-ny) * friction * body.inverseMass
    body.vy += nx * friction * body.inverseMass
    body.spin += tangentialCross * friction * body.inverseInertia
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
    val restitution = if (normalSpeed < -85f * dp) 0.52f else 0f
    val impulse = -(1f + restitution) * normalSpeed /
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
    val tangentSpeed = ((b.vx - b.spin * by) - (a.vx - a.spin * ay)) * tx +
            ((b.vy + b.spin * bx) - (a.vy + a.spin * ax)) * ty
    val friction = (-tangentSpeed /
            (sumMass + tangentA * tangentA * a.inverseInertia + tangentB * tangentB * b.inverseInertia))
        .coerceIn(-impulse * 0.48f, impulse * 0.48f)
    a.vx -= tx * friction * a.inverseMass
    a.vy -= ty * friction * a.inverseMass
    a.spin -= tangentA * friction * a.inverseInertia
    b.vx += tx * friction * b.inverseMass
    b.vy += ty * friction * b.inverseMass
    b.spin += tangentB * friction * b.inverseInertia
}

internal class KineticShapesSimulation(
    var width: Float,
    var height: Float,
    private val dp: Float,
    seed: Int,
    private val contours: List<KineticShapeContour>,
) {
    private val random = Random(seed)
    val bodies = ArrayList<KineticShapeBody>(MaxCount)
    var left = width * 0.20f
        private set
    var right = width * 0.80f
        private set
    var floor = height * 0.75f
        private set
    var gateOpen = false
        private set
    private var elapsed = 0f
    private var spawnElapsed = 0f
    private var spawnedCount = 0
    private var shapeOrder = contours.indices.shuffled(random)
    private val contactScratch = ShapeContact()
    private val sortedIndices = IntArray(MaxCount)
    private val candidatePairs = IntArray(MaxCount * (MaxCount - 1) / 2)

    init {
        var queuedTop = -4f * dp
        repeat(18) {
            val spec = nextBodySpec()
            val availableWidth = right - left - 2f * spec.bound
            val availableHeight = floor - 2f * spec.bound
            if (availableWidth < 0f) return@repeat
            var placed = false
            if (availableHeight > 0f) {
                var attempts = 0
                while (!placed && attempts < 80) {
                    attempts++
                    val x = left + spec.bound + random.nextFloat() * availableWidth
                    val y = spec.bound + random.nextFloat() * availableHeight
                    if (hasRoom(x, y, spec.bound)) {
                        addBody(x, y, spec)
                        placed = true
                    }
                }
            }
            if (!placed) {
                val x = left + spec.bound + random.nextFloat() * availableWidth
                val y = queuedTop - spec.bound
                addBody(x, y, spec)
                queuedTop = y - spec.bound - 4f * dp
            }
        }
    }

    fun resize(newWidth: Float, newHeight: Float) {
        if (newWidth <= 0f || newHeight <= 0f ||
            (width == newWidth && height == newHeight)
        ) return

        val offsetX = (newWidth - width) * 0.5f
        val offsetY = (newHeight - height) * 0.5f
        width = newWidth
        height = newHeight
        val minimumWidth = (bodies.maxOfOrNull { it.broadRadius } ?: 0f) * 2f + 4f * dp
        val enclosureWidth = max(newWidth * 0.60f, minimumWidth)
        left = (newWidth - enclosureWidth) * 0.5f
        right = (newWidth + enclosureWidth) * 0.5f
        floor = newHeight * 0.75f
        for (body in bodies) {
            body.translateBy(offsetX, offsetY)
        }
    }

    private fun nextBodySpec(): BodySpec {
        val shapeIndex = shapeOrder[spawnedCount % contours.size]
        val raw = when (sizeCycle[spawnedCount % sizeCycle.size]) {
            ShapeSize.Small -> 22f + random.nextFloat() * 12f
            ShapeSize.Medium -> 40f + random.nextFloat() * 22f
            ShapeSize.Huge -> 82f + random.nextFloat() * 34f
        } * dp
        val boundRadius = contours[shapeIndex].boundRadius
        val radius = min(raw, (right - left) * 0.46f / boundRadius)
        return BodySpec(shapeIndex, radius, radius * boundRadius)
    }

    private fun hasRoom(x: Float, y: Float, bound: Float): Boolean = bodies.none { body ->
        val dx = x - body.x
        val dy = y - body.y
        val distance = bound + body.broadRadius + 3f * dp
        dx * dx + dy * dy < distance * distance
    }

    private fun addBody(x: Float, y: Float, spec: BodySpec) {
        if (bodies.size >= MaxCount) return
        val body = KineticShapeBody(
            x, y, (random.nextFloat() - 0.5f) * 50f * dp, 0f,
            spec.radius, spec.shapeIndex, random.nextInt(9),
            random.nextFloat() * 2f * PI.toFloat(), 0f, contours[spec.shapeIndex],
        )
        body.rebuild()
        bodies.add(body)
        spawnedCount++
        if (spawnedCount % contours.size == 0) shapeOrder = contours.indices.shuffled(random)
    }

    private fun trySpawn() {
        if (bodies.size >= MaxCount) return
        val spec = nextBodySpec()
        val available = right - left - 2f * spec.bound
        if (available <= 0f) return
        repeat(10) {
            val x = left + spec.bound + random.nextFloat() * available
            val y = -spec.bound - 2f * dp
            if (hasRoom(x, y, spec.bound)) {
                addBody(x, y, spec)
                return
            }
        }
    }

    private fun findCandidates(): Int {
        val count = min(bodies.size, sortedIndices.size)
        for (i in 0 until count) {
            var j = i
            val minY = bodies[i].minY
            while (j > 0 && bodies[sortedIndices[j - 1]].minY > minY) {
                sortedIndices[j] = sortedIndices[j - 1]
                j--
            }
            sortedIndices[j] = i
        }
        val margin = 24f * dp
        var pairCount = 0
        for (i in 0 until count) {
            val ai = sortedIndices[i]
            val a = bodies[ai]
            for (j in i + 1 until count) {
                val bi = sortedIndices[j]
                val b = bodies[bi]
                if (b.minY > a.maxY + margin) break
                if (b.maxX < a.minX - margin || b.minX > a.maxX + margin) continue
                candidatePairs[pairCount++] = ai * MaxCount + bi
            }
        }
        return pairCount
    }

    fun step(dt: Float) {
        elapsed += dt
        gateOpen = (elapsed % CycleSeconds) in OpenAt..CloseAt
        spawnElapsed += dt
        val interval = if (gateOpen) 0.20f else 0.30f
        if (spawnElapsed >= interval) {
            spawnElapsed -= interval
            if (bodies.size < TargetCount || gateOpen) trySpawn()
        }
        val h = dt / 4f
        repeat(4) {
            for (b in bodies) {
                b.vy = min(b.vy + 1250f * dp * h, 1650f * dp)
                b.x += b.vx * h
                b.y += b.vy * h
                b.angle += b.spin * h
                b.spin *= (1f - 4.5f * h).coerceAtLeast(0f)
                if (abs(b.spin) < 0.025f) b.spin = 0f
                b.rebuild()
            }
            val pairCount = findCandidates()
            repeat(4) {
                for (b in bodies) {
                    if (b.minX < left) {
                        wallContact(b, 1f, 0f, left - b.minX, b.leftContactX, b.leftContactY, dp)
                    }
                    if (b.maxX > right) {
                        wallContact(
                            b,
                            -1f,
                            0f,
                            b.maxX - right,
                            b.rightContactX,
                            b.rightContactY,
                            dp
                        )
                    }
                    if (!gateOpen && b.y < floor && b.maxY > floor) {
                        wallContact(
                            b,
                            0f,
                            -1f,
                            b.maxY - floor,
                            b.bottomContactX,
                            b.bottomContactY,
                            dp
                        )
                    }
                }
                for (index in 0 until pairCount) {
                    val pair = candidatePairs[index]
                    val a = bodies[pair / MaxCount]
                    val b = bodies[pair % MaxCount]
                    if (!contact(a, b, contactScratch)) continue
                    resolve(a, b, contactScratch, dp)
                }
            }
        }
        bodies.removeAll { it.minY > height + 8f * dp }
    }
}
