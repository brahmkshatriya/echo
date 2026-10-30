package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.brahmkshatriya.echo.app.ui.components.PolygonIconRipple
import dev.brahmkshatriya.echo.app.ui.components.animatedPolygonIconShape
import dev.brahmkshatriya.echo.app.ui.components.clover6Sided
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_playlist_play
import org.jetbrains.compose.resources.painterResource

@Composable
fun QueueButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = animatedPolygonIconShape(clover6Sided(), MaterialShapes.Square, interaction),
        interactionSource = interaction,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
    ) {
        PolygonIconRipple(interaction) {
            Icon(painterResource(Res.drawable.ic_playlist_play), contentDescription = "Open queue")
        }
    }
}
