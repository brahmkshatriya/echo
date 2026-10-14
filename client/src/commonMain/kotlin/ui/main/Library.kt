package dev.brahmkshatriya.echo.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import dev.brahmkshatriya.echo.app.ui.components.blurFadePagerTransition
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSkipNavigator
import dev.brahmkshatriya.echo.app.ui.player.PlayerItem
import dev.brahmkshatriya.echo.app.ui.player.PlayerControlsState
import dev.brahmkshatriya.echo.app.ui.player.ProvidePlayerControls
import dev.brahmkshatriya.echo.app.ui.player.animatedSkipEntrance

@Composable
fun Library() {
    val artWorks = LocalPlayerItems.current
    val controls = remember(artWorks.size) { PlayerControlsState(artWorks.size) }
    val sheet = LocalPlayerSheet.current
    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
    LaunchedEffect(pagerState, controls) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            controls.currentSong = controls.songAt(page)
        }
    }
    val pageScrolledToTop = remember { mutableStateMapOf<Int, Boolean>() }
    val pagerUserScrollEnabled by remember {
        derivedStateOf {
            pageScrolledToTop[controls.songAt(pagerState.currentPage)] != false
        }
    }
    ProvidePlayerControls(pagerState, controls) {
        val skipNavigator = LocalPlayerSkipNavigator.current
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = pagerUserScrollEnabled,
            key = { controls.songAt(it) },
        ) { page ->
            val song = controls.songAt(page)
            Box(Modifier.fillMaxSize().blurFadePagerTransition(pagerState, page) {
                sheet?.progressState?.floatValue?.coerceIn(0f, 1f) ?: 1f
            }.animatedSkipEntrance(skipNavigator, song)) {
                PlayerItem(song, onScrolledToTopChanged = { scrolledToTop ->
                    pageScrolledToTop[song] = scrolledToTop
                })
            }
        }
    }
}
