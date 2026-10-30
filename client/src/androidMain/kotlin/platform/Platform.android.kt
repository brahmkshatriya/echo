package dev.brahmkshatriya.echo.app.platform

import android.content.ClipData
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

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
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            )
        }
    }

@Composable
actual fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>? =
    WindowInsets.imeAnimationSource to WindowInsets.imeAnimationTarget

@Composable
actual fun KeepScreenAwake(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val wasKeepingScreenOn = view.keepScreenOn
        if (enabled) view.keepScreenOn = true
        onDispose { if (enabled) view.keepScreenOn = wasKeepingScreenOn }
    }
}

actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText("", text)))
}

actual fun ImageBitmap.toByteArray(quality: Int, maxSize: Int): ByteArray {
    require(quality in -1..100) { "Image quality must be -1 or between 0 and 100" }
    require(maxSize > 0) { "Image size must be positive" }
    val format = if (quality == -1) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    val encodingQuality = if (quality == -1) 100 else quality
    val source = asAndroidBitmap()
    val bitmap = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        source.config == Bitmap.Config.HARDWARE
    ) checkNotNull(source.copy(Bitmap.Config.ARGB_8888, false)) {
        "Could not copy hardware bitmap for software rendering"
    } else source
    var resized: Bitmap? = null
    try {
        val scale = (maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        val thumbnail = if (scale == 1f) bitmap else bitmap.scale(
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
        ).also { resized = it }
        return ByteArrayOutputStream().use { output ->
            check(thumbnail.compress(format, encodingQuality, output)) { "Could not encode image" }
            output.toByteArray()
        }
    } finally {
        resized?.takeUnless { it === bitmap }?.recycle()
        if (bitmap !== source) bitmap.recycle()
    }
}
