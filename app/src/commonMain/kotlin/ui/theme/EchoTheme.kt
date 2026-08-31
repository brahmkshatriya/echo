package dev.brahmkshatriya.echo.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.mikepenz.hypnoticcanvas.shaderBackground
import dev.brahmkshatriya.echo.app.ui.components.WavyGrainyShader

val LocalDensityMultiplier = compositionLocalOf { 1f }
val LocalFontScaleMultiplier = compositionLocalOf { 1f }
val LocalCustomTheme = compositionLocalOf<ColorScheme?> { null }
val LocalCustomTypography = compositionLocalOf<Typography?> { null }
val LocalSurfaceColor = compositionLocalOf { Color.Unspecified }

@Composable
fun EchoTheme(
    content: @Composable () -> Unit,
) = CompositionLocalProvider(
    LocalDensity provides LocalDensity.current.run {
        Density(
            density = density * LocalDensityMultiplier.current,
            fontScale = fontScale * LocalFontScaleMultiplier.current
        )
    }
) {
    val isDarkTheme = isSystemInDarkTheme()
    val customTheme = LocalCustomTheme.current
    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = PaletteStyle.FruitSalad,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
        primary = Primary,
        secondary = Secondary,
        tertiary = Tertiary,
    ) {
        customTheme ?: it
    }
    val colorScheme = animateColorScheme(dynamicThemeState.colorScheme)
    val typography = LocalCustomTypography.current ?: googleSansTypography()
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography
    ) {
        Surface(color = colorScheme.surfaceContainer) {
//            ShaderBG(colorScheme)
//            val bg = Color.Black.copy(0.33f)
            val bg = colorScheme.surface
            CompositionLocalProvider(LocalSurfaceColor provides bg) { content() }
        }
    }
}

@Composable
fun ShaderBG(colorScheme: ColorScheme) {
    val perlinCloud = remember {
        WavyGrainyShader(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary
        )
    }
    LaunchedEffect(colorScheme) {
        perlinCloud.changeColor(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(perlinCloud)
    )
    Box(
        modifier = Modifier.fillMaxSize()
            .background(colorScheme.surfaceContainerLowest.copy(0.5f))
    )
}