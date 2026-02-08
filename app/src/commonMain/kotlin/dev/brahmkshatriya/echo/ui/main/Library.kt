package dev.brahmkshatriya.echo.ui.main

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import dev.brahmkshatriya.echo.ui.player.PlayerItem

@Composable
fun Library() {
    val artWorks = listOf(
        "https://i1.sndcdn.com/artworks-f5P5EvBt5Qu57jLk-UNArNA-t1080x1080.jpg",
        "https://i1.sndcdn.com/artworks-mJmURREt59PyaXxx-nhowNw-t1080x1080.png",
        "https://i1.sndcdn.com/artworks-GzqTFOMbFiXRz5LL-G1R9uA-t1080x1080.jpg",
        "https://i1.sndcdn.com/artworks-UbVxfud5u7hzFUPc-pxSyCg-t1080x1080.png",
        "https://i1.sndcdn.com/artworks-7C8GJbswfVyxJ0z6-r5FPkQ-t1080x1080.png"
    )
    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
    HorizontalPager(pagerState) {
        PlayerItem(artWorks[it], it)
    }
}