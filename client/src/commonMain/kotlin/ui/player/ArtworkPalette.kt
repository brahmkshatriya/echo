package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import dev.brahmkshatriya.echo.app.ui.theme.Primary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Lightweight OKLab values for hue, chroma, and population-weighted blending.
 * Compose ColorSpaces.Oklab handles the actual color-space conversions. */
private data class Lab(val l: Double, val a: Double, val b: Double) {
    val chroma get() = sqrt(a * a + b * b)
    val hue get() = atan2(b, a)

    fun withHueOffset(degrees: Double, saturation: Double = chroma): Lab {
        val angle = hue + degrees * PI / 180.0
        return Lab(l, cos(angle) * saturation, sin(angle) * saturation)
    }

    fun withChroma(minimum: Double, maximum: Double = 0.22): Lab {
        val saturation = chroma.coerceIn(minimum, maximum)
        return withHueOffset(0.0, saturation)
    }

    fun distance(other: Lab): Double {
        val dl = (l - other.l) * 0.8
        val da = (a - other.a) * 1.3
        val db = (b - other.b) * 1.3
        return sqrt(dl * dl + da * da + db * db)
    }

    fun tone(value: Double, chromaMultiplier: Double = 1.0): Lab = Lab(
        value.coerceIn(0.0, 1.0), a * chromaMultiplier, b * chromaMultiplier
    )

    fun muted(amount: Double): Lab = Lab(l, a * amount, b * amount)

    fun blend(other: Lab, otherWeight: Double): Lab {
        val weight = otherWeight.coerceIn(0.0, 1.0)
        return Lab(
            l * (1.0 - weight) + other.l * weight,
            a * (1.0 - weight) + other.a * weight,
            b * (1.0 - weight) + other.b * weight,
        )
    }

    fun toColor(): Color {
        // Compose handles OKLab <-> sRGB conversion. Extended sRGB preserves
        // out-of-gamut channel values so they can be checked before clipping.
        fun extendedRgb(chromaScale: Double): Color = Color(
            l.toFloat(), (a * chromaScale).toFloat(), (b * chromaScale).toFloat(),
            colorSpace = ColorSpaces.Oklab,
        ).convert(ColorSpaces.ExtendedSrgb)

        fun inGamut(rgb: Color) =
            rgb.red in 0f..1f && rgb.green in 0f..1f && rgb.blue in 0f..1f

        var factor = 1.0
        if (!inGamut(extendedRgb(factor))) {
            var lo = 0.0
            var hi = 1.0
            repeat(12) {
                val mid = (lo + hi) / 2.0
                if (inGamut(extendedRgb(mid))) lo = mid else hi = mid
            }
            factor = lo
        }
        val rgb = extendedRgb(factor)
        return Color(rgb.red, rgb.green, rgb.blue)
    }
}

private fun Color.toLab(): Lab {
    val oklab = convert(ColorSpaces.Oklab)
    return Lab(oklab.red.toDouble(), oklab.green.toDouble(), oklab.blue.toDouble())
}

private data class Sample(val color: Lab, val population: Int)

/** Convert swatches once; dominant hues and small accents share these values. */
private fun Palette?.samples(): List<Sample> = this?.swatches.orEmpty()
    .filter { it.population > 0 }
    .map { Sample(it.color.toLab(), it.population) }

private fun List<Sample>.averageColor(): Lab? {
    val total = sumOf { it.population }.toDouble()
    if (total == 0.0) return null
    return Lab(
        sumOf { it.color.l * it.population } / total,
        sumOf { it.color.a * it.population } / total,
        sumOf { it.color.b * it.population } / total,
    )
}

/** Find the most prevalent *colored hue family* rather than averaging all
 * album colors together. A wall made of several blue shades should beat
 * smaller red/yellow details even if those are individually more saturated.
 * Group hues independently of lightness, with a gentle cap on chroma weight
 * so a small vivid subject cannot overwhelm a large muted background. */
private fun List<Sample>.dominantColorFamily(): Lab? {
    val samples = this
    val total = samples.sumOf { it.population }.toDouble().coerceAtLeast(1.0)
    fun weight(sample: Sample): Double {
        val color = sample.color
        val chroma = ((color.chroma - 0.02) / 0.04).coerceIn(0.0, 1.0)
        // Tiny RGB differences in shadows have unreliable hue; bright, truly
        // colorful regions are counted without further saturation bonuses.
        val visibility = ((color.l - 0.16) / 0.12).coerceIn(0.0, 1.0)
        return sample.population / total * chroma * visibility
    }
    val weighted = samples.map { it to weight(it) }.filter { it.second > 0.0 }
    if (weighted.isEmpty()) return null

    val hueRadius = 26.0
    val winningHue = weighted.maxByOrNull { (candidate, _) ->
        weighted.sumOf { (sample, weight) ->
            val distance = candidate.color.hueSeparationDegrees(sample.color)
            weight * (1.0 - distance / hueRadius).coerceAtLeast(0.0)
        }
    }?.first?.color ?: return null

    var l = 0.0
    var a = 0.0
    var b = 0.0
    var strength = 0.0
    for ((sample, weight) in weighted) {
        val distance = winningHue.hueSeparationDegrees(sample.color)
        val contribution = weight * (1.0 - distance / hueRadius).coerceAtLeast(0.0)
        l += sample.color.l * contribution
        a += sample.color.a * contribution
        b += sample.color.b * contribution
        strength += contribution
    }
    // If the cover is essentially grayscale, don't turn incidental color
    // noise into the primary background hue.
    return if (strength >= 0.018) Lab(l / strength, a / strength, b / strength) else null
}

private fun Lab.hueSeparationDegrees(other: Lab): Double =
    abs(atan2(sin(hue - other.hue), cos(hue - other.hue))) * 180.0 / PI

/** Treat photographic color casts as neutral when the image is otherwise
 * grayscale. The stricter threshold retains black/white art with a colorful
 * subject; the second catches near-gray shadows and cream whites
 * on monochrome covers without washing out warm-toned art. */
private fun List<Sample>.isMostlyGrayscale(): Boolean {
    val total = sumOf { it.population }.toDouble().coerceAtLeast(1.0)
    val colors = map { it.population to it.color.chroma }
    val grayCoverage = colors.sumOf { (population, chroma) ->
        if (chroma < 0.012) population else 0
    } / total
    val nearGrayCoverage = colors.sumOf { (population, chroma) ->
        if (chroma < 0.020) population else 0
    } / total
    val colorfulCoverage = colors.sumOf { (population, chroma) ->
        if (chroma >= 0.032) population else 0
    } / total
    return grayCoverage >= 0.57 || (nearGrayCoverage >= 0.87 && colorfulCoverage < 0.06)
}

/** Extract small, vivid accent *families* instead of requiring individual
 * swatches to cover a large part of the image. The red type on Ready to Die is
 * split across several tiny palette swatches, while most of its pixels are
 * white or the much less saturated reddish-brown skin tones.
 *
 * Chroma is weighted strongly relative to the dominant cover color, but we
 * require a coherent patch of pixels with the same hue; one noisy pixel is
 * not enough to invent an accent. Muted green details also qualify on covers
 * where the dominant color is itself muted. */
private fun List<Sample>.vibrantArtworkAccents(dominant: Lab, grayscale: Boolean): List<Lab> {
    val samples = this
    val total = samples.sumOf { it.population }.toDouble().coerceAtLeast(1.0)
    val baselineChroma = if (grayscale) 0.055 else dominant.chroma.coerceAtLeast(0.038)
    val minimumChroma = if (grayscale) 0.045 else max(0.024, dominant.chroma * 0.70)
    val eligible = samples.filter { sample ->
        val color = sample.color
        color.chroma >= minimumChroma && color.l in 0.18..0.94 &&
            (color.l >= 0.30 || color.chroma >= 0.10) &&
            (grayscale || color.hueSeparationDegrees(dominant) >= 32.0)
    }
    if (eligible.isEmpty()) return emptyList()

    fun vividWeight(sample: Sample): Double {
        val strength = (sample.color.chroma / baselineChroma).coerceAtMost(5.0)
        return (sample.population / total) * strength * strength * strength
    }
    val hueRadius = 22.0
    val ranked = eligible.map { center ->
        var population = 0.0
        var support = 0.0
        var l = 0.0
        var a = 0.0
        var b = 0.0
        for (sample in eligible) {
            val distance = sample.color.hueSeparationDegrees(center.color)
            val similarity = (1.0 - distance / hueRadius).coerceAtLeast(0.0)
            population += sample.population / total * similarity
            val weight = vividWeight(sample) * similarity
            support += weight
            l += sample.color.l * weight
            a += sample.color.a * weight
            b += sample.color.b * weight
        }
        // Several tiny red-letter swatches together count as a real patch,
        // but not as the dominant color of the artwork.
        val color = if (support > 0.0) Lab(l / support, a / support, b / support)
            else center.color
        val separation = if (grayscale) 1.0 else
            (color.hueSeparationDegrees(dominant) / 70.0).coerceIn(0.35, 1.0)
        Triple(color, support * sqrt(separation), population)
    }.sortedByDescending { it.second }

    val accents = mutableListOf<Lab>()
    for ((color, score, population) in ranked) {
        if (population < 0.0008 || score < 0.00075) continue
        if (accents.any { it.hueSeparationDegrees(color) < 25.0 }) continue
        accents += color
        if (accents.size == 5) break
    }
    return accents
}

/** How strongly a cover is limited to one hue (or no hue at all). Ignore
 * changes in brightness: black and white shades are not new accent colors.
 * This restrains backgrounds and secondary, but not tertiary separation. */
private fun List<Sample>.monochromeRestraint(): Double {
    val total = sumOf { it.population }.toDouble().coerceAtLeast(1.0)
    var colorWeight = 0.0
    var hueX = 0.0
    var hueY = 0.0
    for (cluster in this) {
        val chroma = cluster.color.chroma
        if (chroma < 0.018) continue
        val weight = cluster.population / total *
            ((chroma - 0.018) / 0.075).coerceIn(0.0, 1.0)
        colorWeight += weight
        hueX += cos(cluster.color.hue) * weight
        hueY += sin(cluster.color.hue) * weight
    }
    if (colorWeight < 0.001) return 1.0
    val coherence = (sqrt(hueX * hueX + hueY * hueY) / colorWeight).coerceIn(0.0, 1.0)
    val diversity = colorWeight * (1.0 - coherence)
    // Gradual transition: covers with several substantial, distinct colors
    // retain the existing colorful palette; one-hue covers remain restrained.
    return (1.0 - diversity / 0.25).coerceIn(0.0, 1.0)
}

/** Keep the artwork hue while applying a bounded chroma range. */
private fun Lab.tinted(minChroma: Double, maxChroma: Double, fallback: Lab): Lab {
    val direction = if (chroma >= 0.018) this else fallback
    val amount = chroma.coerceIn(minChroma, maxChroma)
    return Lab(l, cos(direction.hue) * amount, sin(direction.hue) * amount)
}

/** Start from the rendered primary container's hue rather than an unrelated
 * artwork swatch. The final primary is selected for visual contrast below. */
private fun primaryNearContainer(
    main: Lab,
    container: Color,
): Lab {
    val displayed = container.toLab()
    val hue = if (displayed.chroma >= 0.02) displayed.hue else main.hue
    val chroma = max(main.chroma * 1.18, displayed.chroma * 1.25 + 0.015)
        .coerceIn(0.13, 0.23)
    return Lab(displayed.l, cos(hue) * chroma, sin(hue) * chroma)
}

/** Light-mode primaries should be distinct colors, not just darker versions
 * of the container. Evaluate the *displayed* sRGB results: gamut clipping can
 * otherwise make highly saturated greens, cyans and yellows look almost alike.
 * Prefer a bright, colorful nearby hue over simply lowering lightness. */
private fun primaryDistinctFromContainer(source: Lab, container: Color, restraint: Double = 0.0): Color {
    val displayedContainer = container.toLab()
    var best = source.tone(0.74).toColor()
    var bestScore = Double.MAX_VALUE
    val rotations = listOf(0.0, -10.0, 10.0, -20.0, 20.0, -30.0, 30.0, -40.0, 40.0)
    for (rotation in rotations) {
        // Monochrome covers get tonal distinction without fabricated hues.
        val tint = source.withHueOffset(rotation * (1.0 - restraint * 0.85), source.chroma * 1.15)
        for (step in 0..24) {
            val tone = 0.78 - step * 0.01
            val candidate = tint.tone(tone).toColor()
            // Preserve dark onPrimary text in light mode, regardless of hue.
            if (Color.Black.contrastWith(candidate) < 4.5) continue
            val contrast = candidate.contrastWith(container)
            val displayedCandidate = candidate.toLab()
            val distance = displayedCandidate.distance(displayedContainer)
            val chroma = displayedCandidate.chroma
            val score =
                (TARGET_PRIMARY_CONTAINER_CONTRAST - contrast).coerceAtLeast(0.0) * 8.0 +
                (0.145 - distance).coerceAtLeast(0.0) * 12.0 +
                abs(tone - 0.73) * 1.8 +
                abs(rotation) * 0.0025 -
                chroma * 0.45
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
    }
    return best
}

/** Keep text in the artwork hue while avoiding muddy near-black yellows,
 * greens and cyans. Grayscale artwork stays neutral. */
private fun Lab.foregroundTint(darkText: Boolean): Lab {
    if (chroma < 0.012) return muted(0.0)
    val hueDegrees = (hue * 180.0 / PI + 360.0) % 360.0
    val chromaLimit = when {
        darkText && hueDegrees in 65.0..210.0 -> 0.045
        darkText -> 0.065
        else -> 0.075
    }
    return withHueOffset(0.0, chroma.coerceIn(0.02, chromaLimit))
}

/** Darker or lighter artwork-tinted text, selected against the rendered fill. */
private fun tintedForeground(
    background: Color,
    source: Lab = background.toLab(),
    preferDark: Boolean? = null,
    minimumContrast: Double = 4.5,
    preferredTone: Double? = null,
): Color {
    val darkText = preferDark ?: (Color.Black.contrastWith(background) >= Color.White.contrastWith(background))
    val tint = source.foregroundTint(darkText)
    val startTone = preferredTone ?: if (darkText) 0.28 else 0.89
    val step = if (darkText) -0.01 else 0.005
    val count = if (darkText) 24 else 20
    for (index in 0..count) {
        val candidate = tint.tone(startTone + index * step).toColor()
        if (candidate.contrastWith(background) >= minimumContrast) return candidate
    }
    // Extreme cases receive the most contrasting tinted shade, not a pure
    // black or white that breaks the artwork's palette.
    return tint.tone(startTone + count * step).toColor()
}

private data class Role(val color: Color, val onColor: Color, val container: Color, val onContainer: Color)

// Tuning points for the three artwork-derived color roles. Contrast targets
// guide candidate selection; the tertiary foreground minimum is mandatory.
private const val TARGET_PRIMARY_CONTAINER_CONTRAST = 1.65
private const val TARGET_SECONDARY_PRIMARY_CONTRAST = 1.4
private const val TARGET_TERTIARY_PRIMARY_CONTRAST = 1.8
private const val MINIMUM_TERTIARY_BLACK_CONTRAST = 3.5

/** Primary keeps the dominant hue while staying legible against its
 * container. In light mode it also avoids looking like a darker overlay. */
private fun primaryRole(source: Lab, container: Color, hue: Lab, dark: Boolean, restraint: Double): Role {
    val chosen = if (!dark) {
        primaryDistinctFromContainer(source, container, restraint)
    } else {
        // Preserve the established ~5.2:1 primary-to-container target.
        var best = source.tone(0.77).toColor()
        var bestScore = Double.MAX_VALUE
        for (step in 0..96) {
            val tone = 0.49 + step * 0.005
            val candidate = source.tone(tone).toColor()
            val contrast = candidate.contrastWith(container)
            val penalty = when {
                contrast < 3.0 -> (3.0 - contrast) * 8.0
                contrast > 6.2 -> (contrast - 6.2) * 5.0
                else -> 0.0
            }
            val score = abs(contrast - 5.2) + abs(tone - 0.77) * 0.40 + penalty
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
        best
    }
    return Role(
        chosen,
        tintedForeground(chosen, source, preferDark = if (dark) null else true),
        container,
        tintedForeground(container, hue, preferDark = !dark, minimumContrast = 5.0),
    )
}

/** Tertiary's hue and color are already selected by the artwork accent search.
 * Only its container and contrasting on-container color remain to generate. */
private fun tertiaryRole(source: Lab, color: Color, dark: Boolean): Role {
    val container = source.tone(if (dark) 0.30 else 0.79, 1.14).toColor()
    return Role(
        color,
        tintedForeground(color, source, preferDark = true, minimumContrast = MINIMUM_TERTIARY_BLACK_CONTRAST),
        container,
        tintedForeground(container, source, preferDark = !dark, minimumContrast = 5.0),
    )
}

/** Preserve a small artwork accent's actual hue and chroma. Only use the
 * per-artwork seed when no coherent vibrant accent family was extracted. */
private fun contrastingTertiary(
    primary: Role,
    primaryLab: Lab,
    hints: List<Lab>,
    dark: Boolean,
    artworkSeed: Int,
): Pair<Lab, Role> {
    val displayedPrimary = primary.color.toLab()
    val seed = artworkSeed.toLong() and 0xFFFFFFFFL
    // Prefer an actually contrasting hue; if nothing is that far away, still
    // allow nearby genuine colors to contrast through tone rather than seed
    // an unrelated color (e.g. the warm red/brown against Illmatic's gold).
    val separatedHints = hints.filter {
        displayedPrimary.chroma < 0.015 || it.hueSeparationDegrees(displayedPrimary) >= 42.0
    }
    val colors = (separatedHints.ifEmpty { hints }).ifEmpty {
        val hue = if (displayedPrimary.chroma < 0.015) {
            (seed % 360L).toDouble() * PI / 180.0
        } else {
            // For single-hue artwork, choose a sufficiently distant hue,
            // varying the direction and angle across cover identities.
            val direction = if (seed % 2L == 0L) 1.0 else -1.0
            displayedPrimary.hue + direction * (105.0 + seed % 65L) * PI / 180.0
        }
        listOf(Lab(primaryLab.l, cos(hue) * 0.18, sin(hue) * 0.18))
    }
    var best: Pair<Lab, Color>? = null
    var bestScore = Double.MAX_VALUE
    for ((index, hint) in colors.withIndex()) {
        // A vivid red from the image should remain red. Forcing every accent
        // to a high lightness turned Ready to Die's small red type into pink.
        val chroma = if (hints.isEmpty()) 0.18 else hint.chroma.coerceIn(0.075, 0.22)
        for (rotation in listOf(0.0, -4.0, 4.0, -8.0, 8.0)) {
            val tint = hint.withHueOffset(rotation, chroma)
            // Adjust lightness to separate tertiary from primary, but stay
            // close to the original artwork's tone whenever possible.
            for (tone in listOf(0.43, 0.49, 0.55, 0.61, 0.67, 0.73, 0.79, 0.85, 0.91)) {
                val color = tint.tone(tone).toColor()
                val displayed = color.toLab()
                val distance = displayed.distance(displayedPrimary)
                val hueGap = displayed.hueSeparationDegrees(displayedPrimary)
                val contrast = color.contrastWith(primary.color)
                // onTertiary stays dark and artwork-tinted, with the same
                // minimum contrast as the previous pure-black foreground.
                if (Color.Black.contrastWith(color) < MINIMUM_TERTIARY_BLACK_CONTRAST) continue
                // Fast precheck at the darkest allowed ink tone; do not run
                // the full foreground tone search for every accent candidate.
                if (tint.foregroundTint(darkText = true).tone(0.04).toColor().contrastWith(color) <
                    MINIMUM_TERTIARY_BLACK_CONTRAST) continue
                val score =
                    (TARGET_TERTIARY_PRIMARY_CONTRAST - contrast).coerceAtLeast(0.0) * 25.0 +
                    (0.20 - distance).coerceAtLeast(0.0) * 16.0 +
                    (if (hints.isEmpty()) (80.0 - hueGap).coerceAtLeast(0.0) * 0.04 else 0.0) +
                    index * 1.1 + abs(rotation) * 0.035 +
                    abs(tone - hint.l.coerceIn(0.48, 0.75)) * 2.2
                if (score < bestScore) {
                    bestScore = score
                    best = tint to color
                }
            }
        }
    }
    return best?.let { (tint, chosen) ->
        tint to tertiaryRole(tint, chosen, dark)
    } ?: run {
        val fallback = colors.first().withChroma(0.16)
        fallback to tertiaryRole(fallback, fallback.tone(0.90).toColor(), dark)
    }
}

/** Secondary follows the dominant hue, but is softer and tonally separated
 * from primary. Prefer light secondary colors in both themes as before. */
private fun mutedDominantSecondary(dominant: Lab, primary: Color, dark: Boolean): Role {
    val source = dominant.muted(0.62)
    val base = if (source.chroma < 0.02) source else source.muted(0.70)
    val container = base.tone(if (dark) 0.30 else 0.88, 1.14).toColor()
    var chosen = base.tone(0.87).toColor()
    var bestScore = Double.MAX_VALUE
    for (step in 0..43) {
        val tone = 0.55 + step * 0.01
        val candidate = base.tone(tone).toColor()
        if (Color.Black.contrastWith(candidate) < 4.5) continue
        val separation = candidate.contrastWith(primary)
        val score = (TARGET_SECONDARY_PRIMARY_CONTRAST - separation).coerceAtLeast(0.0) * 18.0 +
            abs(tone - (if (dark) 0.88 else 0.90)) * 1.8
        if (score < bestScore) {
            chosen = candidate
            bestScore = score
        }
    }
    return Role(
        chosen,
        tintedForeground(chosen, source),
        container,
        tintedForeground(container, source, preferDark = !dark, minimumContrast = 5.0),
    )
}

private fun Color.contrastWith(other: Color): Double {
    val a = luminance().toDouble()
    val b = other.luminance().toDouble()
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
private fun Color.readableText(): Color =
    if (Color.Black.contrastWith(this) >= Color.White.contrastWith(this)) Color.Black else Color.White

internal data class FavouriteButtonPalette(
    val container: Color,
    val content: Color,
    val checkedContainer: Color,
    val checkedContent: Color,
)

/** Give the heart a consistent rose hue, tempered by the artwork's actual
 * primary container so it doesn't look like an unrelated bright red sticker. */
internal fun Color.favouriteButtonPalette(dark: Boolean): FavouriteButtonPalette {
    val source = toLab()
    val rose = Color(0xFFE47796).toLab().blend(source, 0.18)

    // An unselected heart should retain just a hint of rose, even on highly
    // colorful album art. The checked fill is warmer and more saturated.
    val subtleRose = rose.withChroma(0.045, 0.065)
    val subtleTone = (source.l + if (dark) 0.065 else -0.035)
        .coerceIn(if (dark) 0.30 else 0.72, if (dark) 0.42 else 0.84)
    val unchecked = subtleRose.tone(subtleTone).toColor()
    val iconRose = rose.withChroma(0.09, 0.12)
    var uncheckedIcon = unchecked.readableText()
    for (step in 0..40) {
        val tone = (if (dark) 0.73 + step * 0.006 else 0.42 - step * 0.006)
            .coerceIn(0.16, 0.97)
        val candidate = iconRose.tone(tone).toColor()
        if (candidate.contrastWith(unchecked) >= 4.5) {
            uncheckedIcon = candidate
            break
        }
    }

    val strongRose = rose.withChroma(0.105, 0.145)
    var checkedTone = if (dark) 0.60 else 0.71
    var checked = strongRose.tone(checkedTone).toColor()
    // Make the selected state distinguishable from the primary container,
    // without compromising the contrast of the icon on the rose fill.
    repeat(16) {
        if (checked.contrastWith(this) >= 1.5) return@repeat
        checkedTone += if (dark) 0.01 else -0.01
        checked = strongRose.tone(checkedTone).toColor()
    }
    return FavouriteButtonPalette(
        container = unchecked,
        content = uncheckedIcon,
        checkedContainer = checked,
        checkedContent = checked.readableText(),
    )
}

/** Artwork palette rules:
 * 1. Primary and its container follow the dominant hue family (gray stays gray).
 * 2. Secondary is a brighter, muted version of that same family.
 * 3. Tertiary uses a small vivid artwork accent, or a stable per-cover seed;
 *    it contrasts with primary and always uses dark, artwork-tinted text.
 * Surfaces are neutral shades of the displayed primary container.
 * This only changes the player; the outer app theme is untouched. */
internal fun Palette?.artworkColorScheme(dark: Boolean, artworkSeed: Int = 0): ColorScheme {
    val samples = samples()
    val grayscale = samples.isNotEmpty() && samples.isMostlyGrayscale()
    val restraint = if (samples.isEmpty()) 0.0 else samples.monochromeRestraint()
    val overall = samples.averageColor() ?: Primary.toLab()
    // Favor the largest *hue family*, not an average of unrelated hues.
    val mainSeed = samples.dominantColorFamily() ?: overall
    val hueReference = if (mainSeed.chroma >= 0.02) mainSeed
        else if (overall.chroma >= 0.02) overall else Primary.toLab()
    // Mostly black-and-white artwork must remain grayscale. Only tertiary
    // is allowed to introduce a new color; even a vivid foreground element
    // should not tint the entire player's primary container.
    val mainColor = if (grayscale) Lab(mainSeed.l, 0.0, 0.0) else
        mainSeed.tinted(0.105, 0.21, hueReference).blend(
            mainSeed.tinted(0.025, 0.085, hueReference), restraint
        )
    val mainTone = ((if (dark) 0.27 else 0.81) + (mainSeed.l - 0.50) * 0.06)
        .coerceIn(if (dark) 0.24 else 0.78, if (dark) 0.31 else 0.85)
    val mainContainer = mainColor.tone(mainTone, if (dark) 1.05 else 1.5).toColor()

    val primaryLab = if (grayscale) mainColor else primaryNearContainer(mainColor, mainContainer).blend(
        mainColor.withChroma(0.06, 0.11), restraint
    )
    val primary = primaryRole(primaryLab, mainContainer, mainColor, dark, restraint)
    // Secondary stays in the dominant color family; tertiary is an independent
    // small accent, seeded only when the artwork has no eligible accent.
    val secondaryLab = mainColor.muted(0.62)
    val secondary = mutedDominantSecondary(mainColor, primary.color, dark)
    val tertiaryHints = samples.vibrantArtworkAccents(mainSeed, grayscale)
    val (tertiaryLab, tertiary) = contrastingTertiary(primary, primaryLab, tertiaryHints, dark, artworkSeed)

    // Keep the displayed primary container's hue in every neutral. Dark
    // surfaces sit below the container, while light surfaces stay nearly white
    // with only a subtle tint. Preserve a wider gap to surfaceVariant.
    val containerLab = primary.container.toLab()
    val neutral = if (grayscale) containerLab else if (dark) containerLab.muted(0.60)
        else containerLab.withChroma(0.055, 0.085).blend(containerLab.muted(0.65), restraint)
    val neutralStartTone = if (dark) {
        (containerLab.l - 0.11).coerceIn(0.15, 0.19)
    } else {
        (containerLab.l + 0.14).coerceIn(0.94, 0.97)
    }
    var darkestLightTone = (containerLab.l - 0.06).coerceIn(0.72, 0.78)
    if (!dark) {
        // onSurface is reused on elevated surfaces: keep even the darkest
        // light-mode shade readable with black foreground text.
        while (Color.Black.contrastWith(neutral.tone(darkestLightTone).toColor()) < 4.5) {
            darkestLightTone += 0.005
        }
    }
    val neutralEndTone = if (dark) {
        (containerLab.l + 0.035).coerceIn(0.29, 0.34)
    } else darkestLightTone
    fun surface(level: Double) = neutral.tone(
        neutralStartTone + (neutralEndTone - neutralStartTone) * level
    ).toColor()
    val background = surface(if (dark) 0.10 else 0.06)
    val surface = surface(if (dark) 0.18 else 0.16)
    // Push the variant toward the opposite end of the neutral ramp, making it
    // visibly distinct from surface instead of a near-identical adjacent shade.
    val surfaceVariant = surface(if (dark) 0.92 else 0.90)
    // Inverse colors must remain on the opposite side of the luminance range.
    val inverseSurface = neutral.tone(if (dark) 0.87 else 0.25).toColor()
    val primaryFixed = primaryLab.tone(0.87, 0.78).toColor()
    val secondaryFixed = secondaryLab.tone(0.87, 0.78).toColor()
    val tertiaryFixed = tertiaryLab.tone(0.87, 0.78).toColor()

    val defaults = if (dark) darkColorScheme() else lightColorScheme()
    return defaults.copy(
        primary = primary.color,
        onPrimary = primary.onColor,
        primaryContainer = primary.container,
        onPrimaryContainer = primary.onContainer,
        inversePrimary = primaryLab.tone(if (dark) 0.43 else 0.78).toColor(),
        secondary = secondary.color,
        onSecondary = secondary.onColor,
        secondaryContainer = secondary.container,
        onSecondaryContainer = secondary.onContainer,
        tertiary = tertiary.color,
        onTertiary = tertiary.onColor,
        tertiaryContainer = tertiary.container,
        onTertiaryContainer = tertiary.onContainer,
        background = background,
        onBackground = tintedForeground(background, neutral),
        surface = surface,
        onSurface = tintedForeground(surface, neutral),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = tintedForeground(surfaceVariant, neutral),
        surfaceTint = primary.color,
        surfaceDim = surface(if (dark) 0.05 else 0.92),
        surfaceBright = surface(if (dark) 0.96 else 0.02),
        surfaceContainerLowest = surface(0.0),
        surfaceContainerLow = surface(0.25),
        surfaceContainer = surface(0.50),
        surfaceContainerHigh = surface(0.75),
        surfaceContainerHighest = surface(1.0),
        inverseSurface = inverseSurface,
        inverseOnSurface = tintedForeground(inverseSurface, neutral),
        outline = neutral.tone(if (dark) 0.58 else 0.43).toColor(),
        outlineVariant = neutral.tone(if (dark) 0.43 else 0.61).toColor(),
        primaryFixed = primaryFixed,
        primaryFixedDim = primaryLab.tone(0.78, 0.88).toColor(),
        onPrimaryFixed = tintedForeground(primaryFixed, primaryLab, preferDark = true),
        onPrimaryFixedVariant = tintedForeground(primaryFixed, primaryLab,
            preferDark = true, preferredTone = 0.33),
        secondaryFixed = secondaryFixed,
        secondaryFixedDim = secondaryLab.tone(0.78, 0.88).toColor(),
        onSecondaryFixed = tintedForeground(secondaryFixed, secondaryLab, preferDark = true),
        onSecondaryFixedVariant = tintedForeground(secondaryFixed, secondaryLab,
            preferDark = true, preferredTone = 0.33),
        tertiaryFixed = tertiaryFixed,
        tertiaryFixedDim = tertiaryLab.tone(0.78, 0.88).toColor(),
        onTertiaryFixed = tintedForeground(tertiaryFixed, tertiaryLab, preferDark = true),
        onTertiaryFixedVariant = tintedForeground(tertiaryFixed, tertiaryLab,
            preferDark = true, preferredTone = 0.33),
        onError = tintedForeground(defaults.error, defaults.error.toLab()),
        onErrorContainer = tintedForeground(defaults.errorContainer, defaults.error.toLab()),
    )
}
