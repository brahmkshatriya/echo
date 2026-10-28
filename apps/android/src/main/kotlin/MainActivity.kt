package dev.brahmkshatriya.echo

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import dev.brahmkshatriya.echo.app.playback.PlaybackComponentOwner
import dev.brahmkshatriya.echo.app.playback.PlaybackComponent
import dev.brahmkshatriya.echo.app.playback.createPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.App
import dev.brahmkshatriya.echo.app.ui.theme.LocalCustomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES
        val transparentNavigationBar = if (darkMode) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(navigationBarStyle = transparentNavigationBar)
        val playback = (application as PlaybackComponentOwner).playbackComponent
        setContent { AppContent(playback) }
    }
}

@Composable
fun AppContent(playback: PlaybackComponent) {
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val isDark = isSystemInDarkTheme()
        remember(context, isDark) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    } else null
    CompositionLocalProvider(LocalCustomTheme provides scheme) { App(playback) }
}

@Preview(
    showBackground = false,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:parent=pixel_5,navigation=buttons"
)
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val playback = remember(context) { createPlaybackComponent(context, R.drawable.ic_mono) }
    DisposableEffect(playback) { onDispose { playback.close() } }
    AppContent(playback)
}
