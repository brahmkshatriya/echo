package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Crossfade changing icons with the fast spatial motion used by the extension FAB. */
@Composable
fun SwitchingIcon(
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Crossfade(
        targetState = icon,
        modifier = modifier,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Icon switch",
    ) { current ->
        Icon(
            painter = painterResource(current),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
