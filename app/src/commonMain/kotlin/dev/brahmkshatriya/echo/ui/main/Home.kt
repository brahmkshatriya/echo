package dev.brahmkshatriya.echo.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.brahmkshatriya.echo.ui.Media
import dev.brahmkshatriya.echo.ui.components.BetterImage
import dev.brahmkshatriya.echo.ui.components.CircleCutoutShape
import dev.brahmkshatriya.echo.ui.components.FastScrollbar
import dev.brahmkshatriya.echo.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.ui.components.PaddingRoundedCornerShape
import dev.brahmkshatriya.echo.ui.components.StickyHeaderList
import dev.brahmkshatriya.echo.ui.components.materialGroup
import dev.brahmkshatriya.echo.ui.components.rememberBasicScrollbarThumbMover
import dev.brahmkshatriya.echo.ui.components.scrollbarState
import dev.brahmkshatriya.echo.ui.theme.LocalSurfaceColor
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_back
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun Header(i: String) {
    val list = remember(i) { listOf("Header $i", "Apple", "Banana", "Cinnamon") }
    val selected = remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()

    val canScrollBackward by remember {
        derivedStateOf { lazyListState.canScrollBackward }
    }
    val canScrollForward by remember {
        derivedStateOf { lazyListState.canScrollForward }
    }
    val coroutineScope = rememberCoroutineScope()

    Box {
        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(12.dp, 4.dp),
        ) {
            items(list.size) { i ->
                ToggleButton(
                    checked = i == selected.intValue,
                    onCheckedChange = {
                        if (it) selected.intValue = i
                    },
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                ) {
                    Text(list[i])
                }
            }
        }

        AnimatedVisibility(
            canScrollBackward,
            Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp),
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            (lazyListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        )
                    }
                },
                colors = filledIconButtonColors(colorScheme.secondary),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_back),
                    contentDescription = "Scroll left"
                )
            }
        }

        AnimatedVisibility(
            canScrollForward,
            Modifier.align(Alignment.CenterEnd).padding(horizontal = 8.dp),
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            (lazyListState.firstVisibleItemIndex + 1).coerceAtMost(list.size - 1)
                        )
                    }
                },
                colors = filledIconButtonColors(colorScheme.secondary),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_back),
                    modifier = Modifier.scale(-1f),
                    contentDescription = "Scroll right"
                )
            }
        }
    }
}

@Composable
fun Home() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Unspecified,
        contentColor = LocalContentColor.current,
        topBar = {
            TopAppBar(
                expandedHeight = 56.dp,
                colors = TopAppBarColors(
                    containerColor = Color.Unspecified,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = LocalContentColor.current,
                    titleContentColor = LocalContentColor.current,
                    actionIconContentColor = LocalContentColor.current,
                    subtitleContentColor = LocalContentColor.current
                ),
                title = {
                    Text(buildAnnotatedString {
                        append("Good Afternoon, ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.primary
                            )
                        ) {
                            append("Shivam")
                        }
                    }, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                },
//                navigationIcon = {
//                    val backStack = LocalMainBackStack.current
//                    val visible = if (backStack != null) backStack.size > 1 else false
//                    AnimatedVisibility(visible) {
//                        IconButton({
//                            backStack?.removeLastOrNull()
//                        }, shapes = IconButtonDefaults.shapes()) {
//                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
//                        }
//                    }
//                },
                actions = {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        IconButton(
                            onClick = { },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier
                        ) {
                            BetterImage(
                                model = { "https://avatars.githubusercontent.com/u/69040506" },
                                "Shivam",
                                modifier = Modifier.clip(CircleCutoutShape(16.dp, 2.dp, 2.dp))
                                    .padding(4.dp).clip(Circle.toShape())
                            )
                        }
                        BetterImage(
                            model = { "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN" },
                            "Spotify",
                            modifier = Modifier.padding(8.dp).size(12.dp).clip(Circle.toShape())
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                contentPadding = TopAppBarDefaults.ContentPadding
            )
        },
        floatingActionButton = {

        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(bottom = 8.dp)) {
            val listState = rememberLazyListState()
            val scrollbarState = listState.scrollbarState(itemsAvailable = 45)

            val backStack = LocalMainBackStack.current
            val cardColors = CardDefaults.cardColors(
                containerColor = LocalSurfaceColor.current,
            )
            StickyHeaderList(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                isStickyHeaderItem = {
                    it.key !is Int
                },
                stickyHeader = stickyHeader@{ _, _, contentType ->
                    Header(contentType.toString())
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().clip(
                        PaddingRoundedCornerShape(
                            horizontalPadding = 8.dp,
                            topPadding = 56.dp,
                            cornerRadius = 16.dp
                        )
                    )
                ) {
                    (0..4).forEach { i ->
                        item("Header $i", i) {
                            Header(i.toString())
                        }
                        materialGroup {
                            (0..10).forEach {
                                card(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    key = "$i$it",
                                    contentType = i,
                                    colors = cardColors
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                backStack?.add(Media(it.toString()))
                                            }.padding(16.dp, 24.dp)
                                    ) { Text("Item $it") }
                                }
                            }
                        }
                    }
                }
            }
            FastScrollbar(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .padding(end = 4.dp)
                    .padding(innerPadding)
                    .align(Alignment.TopEnd),
                state = scrollbarState,
                scrollInProgress = listState.isScrollInProgress,
                orientation = Orientation.Vertical,
                onThumbMoved = listState.rememberBasicScrollbarThumbMover()
            )
        }
    }
}