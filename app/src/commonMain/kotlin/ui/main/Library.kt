package dev.brahmkshatriya.echo.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.brahmkshatriya.echo.app.ui.components.blurFadePagerTransition
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.PlayerItem

@Composable
fun Library() {
    val artWorks = LocalPlayerItems.current
    val sheet = LocalPlayerSheet.current
    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
    val pageScrolledToTop = remember { mutableStateMapOf<Int, Boolean>() }
    val pagerUserScrollEnabled by remember {
        derivedStateOf {
            pageScrolledToTop[pagerState.currentPage] != false
        }
    }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = pagerUserScrollEnabled,
    ) { page ->
        Box(Modifier.fillMaxSize().blurFadePagerTransition(pagerState, page) {
            sheet?.progressState?.floatValue?.coerceIn(0f, 1f) ?: 1f
        }) {
            PlayerItem(page) { scrolledToTop ->
                pageScrolledToTop[page] = scrolledToTop
            }
        }
    }
}
