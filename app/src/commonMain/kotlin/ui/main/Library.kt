package dev.brahmkshatriya.echo.app.ui.main

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.brahmkshatriya.echo.app.ui.components.depthPagerTransition
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.PlayerItem

@Composable
fun Library() {
    val artWorks = LocalPlayerItems.current
    val sheetProgress = LocalPlayerSheet.current?.progressState?.floatValue?.coerceIn(0f, 1f) ?: 1f
    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
    HorizontalPager(pagerState) { page ->
        Box(Modifier.fillMaxSize().depthPagerTransition(pagerState, page, sheetProgress)) {
            PlayerItem(page)
        }
    }
}
