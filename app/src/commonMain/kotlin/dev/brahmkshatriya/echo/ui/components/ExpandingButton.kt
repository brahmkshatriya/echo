package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times

@Composable
fun Modifier.expandingButton(
    interactionSource: MutableInteractionSource,
    increase: Float = 1.1f
) = run {
    val isPressed by interactionSource.collectIsPressedAsState()
    val widthState = remember { mutableStateOf<Dp?>(null) }
    val animatedWidth by animateDpAsState(
        targetValue = widthState.value?.let {
            if (isPressed) increase * it else it
        } ?: Dp.Unspecified,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "PressExpansion"
    )

    val density = LocalDensity.current
    val modifier = onSizeChanged {
        if (widthState.value != null) return@onSizeChanged
        widthState.value = density.run { it.width.toDp() }
    }
    if (animatedWidth == Dp.Unspecified) return@run modifier
    modifier.width(maxOf(animatedWidth, widthState.value ?: Dp.Unspecified))
}

