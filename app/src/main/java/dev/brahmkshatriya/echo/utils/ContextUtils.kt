package dev.brahmkshatriya.echo.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

object ContextUtils {
    fun appVersion() = BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE

    fun Context.copyToClipboard(label: String?, string: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, string)
        clipboard.setPrimaryClip(clip)
    }

    fun <T> LifecycleOwner.observe(flow: Flow<T>, block: suspend (T) -> Unit) =
        lifecycleScope.launch {
            flow.flowWithLifecycle(lifecycle).collectLatest(block)
        }

    fun <T> LifecycleOwner.collect(flow: Flow<T>, block: suspend (T) -> Unit) =
        lifecycleScope.launch {
            flow.collect {
                runCatching { block(it) }
            }
        }

    fun <T> Context.listenFuture(future: ListenableFuture<T>, block: (Result<T>) -> Unit) {
        future.addListener({
            val result = runCatching { future.get() }
            block(result)
        }, ContextCompat.getMainExecutor(this))
    }

    fun <T> LifecycleOwner.emit(flow: MutableSharedFlow<T>, value: T) {
        lifecycleScope.launch {
            flow.emit(value)
        }
    }

    const val SETTINGS_NAME = "settings"
    fun Context.getSettings() = getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)!!

    private fun Context.getTempDir() = cacheDir.resolve("apks").apply { mkdirs() }
    fun Context.getTempApkFile(): File =
        File.createTempFile("temp", ".apk", getTempDir())

    fun Context.cleanupTempApks() {
        getTempDir().deleteRecursively()
    }
}