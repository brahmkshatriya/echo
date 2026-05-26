package dev.brahmkshatriya.echo.app.ui.main

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.PlayerItem

@Composable
fun Library() {
    val artWorks = LocalPlayerItems.current
    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
    HorizontalPager(pagerState) {
        PlayerItem(it)
    }
}