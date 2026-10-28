package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import dev.brahmkshatriya.echo.app.ui.components.clover6Sided
import dev.brahmkshatriya.echo.app.ui.player.components.*
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_playlist_play
import org.jetbrains.compose.resources.painterResource

private object QueueOverlayPageInfo : NavigationEventInfo()
private object QueuePlayerInfo : NavigationEventInfo()

@Composable
internal fun SharedPlayerQueueOverlay(
    transition: PlayerPillTransition,
    viewport: Size,
    currentEntryId: Int,
    onEntrySelected: (Int) -> Unit,
) {
    PlayerPillOverlay(
        transition = transition,
        viewport = viewport,
        isCurrentPage = true,
        pillColor = colorScheme.secondary,
        collapsedCornerRadius = 12.dp,
        collapsedPolygon = clover6Sided(),
        openInfo = QueueOverlayPageInfo,
        closedInfo = QueuePlayerInfo,
        currentSource = { transition.source },
        alternateSource = transition.alternateSource,
        onAlternateClick = { transition.onAlternateClick?.invoke() },
        pillContent = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_playlist_play),
                    contentDescription = null,
                    tint = colorScheme.onSecondary,
                )
            }
        },
        pageContent = { interactive ->
            QueuePage(
                currentEntryId = currentEntryId,
                interactive = interactive,
                onBack = transition::close,
                onEntrySelected = onEntrySelected,
            )
        },
    )
}
