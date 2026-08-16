package dev.brahmkshatriya.echo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalPlatformAccentColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.brahmkshatriya.echo.app.ui.App
import dev.brahmkshatriya.echo.app.ui.theme.LocalCustomTheme
import dev.brahmkshatriya.echo.app.ui.theme.LocalCustomTypography
import dev.brahmkshatriya.echo.app.ui.theme.LocalDensityMultiplier
import dev.brahmkshatriya.echo.theme.googleSansTypography
import echo.app.generated.resources.Res
import echo.app.generated.resources.app_name
import echo.app.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val InitialDensityMultiplier = 1f
private const val DensityScrollStep = 0.005f
private const val MinDensityMultiplier = 0.5f
private const val MaxDensityMultiplier = 2f

fun main() = application {
    var densityMultiplier by remember { mutableFloatStateOf(InitialDensityMultiplier) }
    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(
            (960 * InitialDensityMultiplier).dp,
            (640 * InitialDensityMultiplier).dp
        )
    )
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.compose_multiplatform),
    ) {
        val accentColor = LocalPlatformAccentColor.current
        val dynamicTheme = accentColor?.let {
            dynamicColorScheme(
                primary = it,
                isDark = isSystemInDarkTheme(),
                style = PaletteStyle.Rainbow,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            )
        }
        CompositionLocalProvider(
            LocalCustomTheme provides dynamicTheme,
            LocalDensityMultiplier provides densityMultiplier,
            LocalCustomTypography provides googleSansTypography()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onCtrlScrollDensityChange {
                        densityMultiplier = (densityMultiplier - it * DensityScrollStep)
                            .coerceIn(MinDensityMultiplier, MaxDensityMultiplier)
                    }
            ) {
                App()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.onCtrlScrollDensityChange(onScroll: (Float) -> Unit): Modifier {
    return onPointerEvent(PointerEventType.Scroll, PointerEventPass.Initial) { event ->
        if (!event.keyboardModifiers.isCtrlPressed) return@onPointerEvent

        val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
        if (scrollDelta == 0f) return@onPointerEvent

        event.changes.forEach { it.consume() }
        onScroll(scrollDelta)
    }
}
