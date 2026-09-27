@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

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
