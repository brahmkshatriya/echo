package dev.brahmkshatriya.echo.app.platform

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
