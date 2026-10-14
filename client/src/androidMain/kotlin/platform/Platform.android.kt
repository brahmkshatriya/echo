package dev.brahmkshatriya.echo.app.platform

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

class AndroidPlatform : Platform {
    override val name = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
@get:Composable
actual val hasTouchInput: State<Boolean>
    get() {
        val context = LocalContext.current
        return remember(context) {
            mutableStateOf(
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) true
                else context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            )
        }
    }

@Composable
internal actual fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>? =
    WindowInsets.imeAnimationSource to WindowInsets.imeAnimationTarget
@Composable
internal actual fun KeepScreenAwake(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val wasKeepingScreenOn = view.keepScreenOn
        if (enabled) view.keepScreenOn = true
        onDispose {
            if (enabled) view.keepScreenOn = wasKeepingScreenOn
        }
    }
}