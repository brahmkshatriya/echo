package dev.brahmkshatriya.echo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalPlatformAccentColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.TitleBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.brahmkshatriya.echo.app.ui.App
import dev.brahmkshatriya.echo.app.ui.theme.LocalCustomTheme
import dev.brahmkshatriya.echo.app.ui.theme.LocalDensityMultiplier
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
    val escapeNavigationInput = remember { EscapeNavigationInput() }
    var densityMultiplier by remember { mutableFloatStateOf(InitialDensityMultiplier) }
    val isDarkTheme = isSystemInDarkTheme()
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
        titleBar = TitleBar.Auto(foreground = if (isDarkTheme) Color.White else Color.Black),
        onKeyEvent = { event ->
            if (event.key != Key.Escape) false else {
                if (event.type == KeyEventType.KeyDown) escapeNavigationInput.back()
                true
            }
        },
    ) {
        val navigationEventDispatcherOwner = LocalNavigationEventDispatcherOwner.current
        DisposableEffect(navigationEventDispatcherOwner, escapeNavigationInput) {
            val dispatcher = navigationEventDispatcherOwner?.navigationEventDispatcher
            dispatcher?.addInput(escapeNavigationInput)
            onDispose { dispatcher?.removeInput(escapeNavigationInput) }
        }
        val accentColor = LocalPlatformAccentColor.current
        val dynamicTheme = accentColor?.let {
            dynamicColorScheme(
                primary = it,
                isDark = isDarkTheme,
                style = PaletteStyle.Rainbow,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            )
        }
        CompositionLocalProvider(
            LocalCustomTheme provides dynamicTheme,
            LocalDensityMultiplier provides densityMultiplier
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

private class EscapeNavigationInput : NavigationEventInput() {
    private var hasEnabledHandlers = false
    fun back() { if (hasEnabledHandlers) dispatchOnBackCompleted() }
    override fun onRemoved() { hasEnabledHandlers = false }
    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        this.hasEnabledHandlers = hasEnabledHandlers
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
