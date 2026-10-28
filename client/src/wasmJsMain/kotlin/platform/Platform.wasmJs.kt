@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

private object WasmPlatform : Platform {
    override val name: String = "WebAssembly"
}

actual fun getPlatform(): Platform = WasmPlatform

@get:Composable
actual val hasTouchInput: State<Boolean>
    get() {
        val state = remember { mutableStateOf(browserHasTouchInput()) }
        DisposableEffect(state) {
            val stopObserving = observeTouchInputChanges {
                state.value = browserHasTouchInput()
            }
            state.value = browserHasTouchInput()
            onDispose { stopObserving() }
        }
        return state
    }

private fun browserHasTouchInput(): Boolean = js("navigator.maxTouchPoints > 0")

@Suppress("unused")
private fun observeTouchInputChanges(onChange: () -> Unit): () -> Unit = js(
    """(() => {
        const pointerQuery = window.matchMedia('(any-pointer: coarse)');
        const refresh = () => onChange();
        pointerQuery.addEventListener('change', refresh);
        window.addEventListener('focus', refresh);
        window.addEventListener('pointerdown', refresh);
        return () => {
            pointerQuery.removeEventListener('change', refresh);
            window.removeEventListener('focus', refresh);
            window.removeEventListener('pointerdown', refresh);
        };
    })()"""
)

@Composable
internal actual fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>? = null

@Composable
internal actual fun KeepScreenAwake(enabled: Boolean) {
    DisposableEffect(enabled) {
        val release = if (enabled) acquireScreenWakeLock() else null
        onDispose { release?.invoke() }
    }
}

/** The Screen Wake Lock API is optional and works only in secure contexts.
 * Re-acquire after the tab becomes visible because browsers release the lock
 * when the document is hidden. Release on disposal, including pending requests. */
@Suppress("unused")
private fun acquireScreenWakeLock(): () -> Unit = js(
    """(() => {
        let active = true;
        let sentinel = null;
        const request = async () => {
            if (!active || document.visibilityState !== 'visible' || !navigator.wakeLock) return;
            try {
                const acquired = await navigator.wakeLock.request('screen');
                if (!active) { acquired.release(); return; }
                sentinel = acquired;
            } catch (_) { /* Wake locks may be unavailable or denied. */ }
        };
        const onVisibility = () => {
            if (document.visibilityState === 'visible') request();
        };
        document.addEventListener('visibilitychange', onVisibility);
        request();
        return () => {
            active = false;
            document.removeEventListener('visibilitychange', onVisibility);
            if (sentinel) sentinel.release();
        };
    })()"""
)

internal actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipEntry.withPlainText(text))
}