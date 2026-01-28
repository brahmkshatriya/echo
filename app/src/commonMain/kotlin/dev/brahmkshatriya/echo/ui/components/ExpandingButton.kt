package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.shapes
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
fun ExpandingButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val widthState = remember { mutableStateOf<Dp?>(null) }
    val animatedWidth by animateDpAsState(
        targetValue = widthState.value?.let {
            if (isPressed) 1.1f * it else it
        } ?: Dp.Unspecified,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "PressExpansion"
    )

    val density = LocalDensity.current
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shapes = shapes(),
        modifier = modifier.onSizeChanged {
            if (widthState.value != null) return@onSizeChanged
            widthState.value = density.run { it.width.toDp() }
        }.then(
            if (animatedWidth == Dp.Unspecified) Modifier
            else Modifier.width(maxOf(animatedWidth, widthState.value ?: Dp.Unspecified))
        ),
        content = content
    )
}

