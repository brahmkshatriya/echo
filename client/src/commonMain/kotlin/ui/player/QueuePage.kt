package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_back
import echo.client.generated.resources.ic_queue_music
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun QueuePage(
    currentSong: Int,
    songCount: Int,
    topPadding: Dp,
    onBack: () -> Unit,
    onSongSelected: (Int) -> Unit,
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Queue") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = interactive) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = "Close queue",
                        )
                    }
                },
                modifier = Modifier.padding(top = topPadding),
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            userScrollEnabled = interactive,
        ) {
            if (songCount > 0) {
                item(key = "queue-current-label") {
                    Text(
                        text = "Now playing",
                        style = typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                item(key = "queue-current") {
                    QueueSongRow(currentSong, playing = true, enabled = interactive, onClick = {})
                }
                item(key = "queue-separator") {
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                    Text(
                        text = "Up next",
                        style = typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
                items((songCount - 1).coerceAtLeast(0), key = { "queue-song-$it" }) { offset ->
                    val index = (currentSong + offset + 1) % songCount
                    QueueSongRow(
                        index,
                        playing = false,
                        enabled = interactive,
                        onClick = { onSongSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSongRow(index: Int, playing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        supportingContent = { Text(if (playing) "Artist $index · Playing now" else "Artist $index") },
        leadingContent = {
            Icon(
                painter = painterResource(Res.drawable.ic_queue_music),
                contentDescription = null,
                tint = if (playing) colorScheme.primary else colorScheme.onSurfaceVariant,
            )
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled && !playing, onClick = onClick),
    ) { Text("Song $index") }
}
