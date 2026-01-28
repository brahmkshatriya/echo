package dev.brahmkshatriya.echo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.brahmkshatriya.betterwindow.BetterWindow
import dev.brahmkshatriya.betterwindow.platform.LocalPlatformWindow
import dev.brahmkshatriya.betterwindow.platform.overrideTitleBarAppearance
import dev.brahmkshatriya.echo.theme.googleSansTypography
import dev.brahmkshatriya.echo.ui.App
import dev.brahmkshatriya.echo.ui.theme.LocalCustomTheme
import dev.brahmkshatriya.echo.ui.theme.LocalCustomTypography
import echo.app.generated.resources.Res
import echo.app.generated.resources.app_name
import echo.app.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(400.dp, 600.dp)
    )
    BetterWindow(
        ::exitApplication,
        windowState = windowState,
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.compose_multiplatform),
    ) {
        val accent by LocalPlatformWindow.current.accentColor.collectAsState()
        val dynamicTheme = if(accent != Color.Unspecified) {
            dynamicColorScheme(
                primary = accent,
                isDark = isSystemInDarkTheme(),
                style = PaletteStyle.Rainbow,
                specVersion = ColorSpec.SpecVersion.SPEC_2021
            )
        } else null
        CompositionLocalProvider(
            LocalCustomTheme provides dynamicTheme,
            LocalCustomTypography provides googleSansTypography()
        ) {
            overrideTitleBarAppearance(isSystemInDarkTheme())
            App()
        }
    }
}