package dev.brahmkshatriya.echo.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.iconColor
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import dev.brahmkshatriya.echo.ui.components.BetterImage
import dev.brahmkshatriya.echo.ui.components.paddingMask
import dev.brahmkshatriya.echo.ui.components.simpleTween
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_close
import echo.app.generated.resources.ic_extension
import echo.app.generated.resources.ic_more_vert
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainSideNavigation(
    isVisible: Boolean,
    wasVisible: Boolean,
    sheetPadding: Dp,
    sheetProgress: MutableFloatState,
    selected: MainRoute?,
    bottomPaddingState: MutableState<Dp>,
    startPaddingState: MutableState<Dp>,
    onSelected: (MainRoute) -> Unit,
) {
    val isVisibleState = remember { mutableStateOf(isVisible) }
    val wasVisibleState = remember { mutableStateOf(wasVisible) }
    SideEffect {
        isVisibleState.value = isVisible
        wasVisibleState.value = wasVisible
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().paddingMask()) {
        startPaddingState.value = if (isVisibleState.value && maxWidth > 560.dp) 72.dp else 0.dp
        bottomPaddingState.value = if (isVisibleState.value && maxWidth < 560.dp) 64.dp else 0.dp

        val animated = remember { Animatable(0f) }
        val state = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
            ?.transitionState
        LaunchedEffect(state, wasVisible) {
            state?.collect {
                when (it) {
                    is NavigationEventTransitionState.InProgress -> if (wasVisible)
                        animated.snapTo(1 - it.latestEvent.progress)

                    else -> animated.animateTo(if (isVisible) 0f else 1f, simpleTween())
                }
            }
        }

        val systemBars = WindowInsets.systemBars.asPaddingValues()


        AnimatedVisibility(
            remember(maxWidth) { maxWidth < 560.dp },
            Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(simpleTween()) + slideInVertically(simpleTween()) { it },
            exit = fadeOut(simpleTween()) + slideOutVertically(simpleTween()) { it }
        ) {
            NavigationBar(
                Modifier
                    .height(64.dp + systemBars.calculateBottomPadding())
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        val positiveProgress = sheetProgress.floatValue.coerceAtLeast(0f)
                        val transitionValue = maxOf(positiveProgress, animated.value)
                        alpha = 1 - transitionValue * 1.15f
                        translationY = size.height * transitionValue
                    },
                containerColor = Color.Unspecified,
                contentColor = LocalContentColor.current
            ) {
                MainRoute.entries.forEachIndexed { i, it ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painterResource(if (selected == it) it.selected else it.unselected),
                                contentDescription = it.name,
                            )
                        },
                        label = {
                            Text(it.name, Modifier.offset(y = -(2).dp))
                        },
                        selected = selected == it,
                        onClick = { onSelected(it) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colorScheme.onPrimaryFixedVariant,
                            indicatorColor = colorScheme.primaryFixedDim
                        ),
                        modifier = Modifier.graphicsLayer {
                            val positiveProgress = sheetProgress.floatValue.coerceAtLeast(0f)
                            val transitionValue = maxOf(positiveProgress, animated.value)
                            translationY = transitionValue * size.height * i / 2
                        }
                    )
                }
            }
        }
        AnimatedVisibility(
            maxWidth > 560.dp,
            enter = fadeIn(simpleTween()) + slideInHorizontally(simpleTween()) { -it },
            exit = fadeOut(simpleTween()) + slideOutHorizontally(simpleTween()) { -it }
        ) {
            NavigationRail(
                Modifier.padding(top = 8.dp)
                    .graphicsLayer {
                        val positiveProgress = sheetProgress.floatValue.coerceAtLeast(0f)
                        val transitionValue = maxOf(positiveProgress, animated.value)
                        alpha = 1 - transitionValue * 1.15f
                        translationX = -size.width * transitionValue
                        translationY = -(size.height - sheetPadding.toPx()) * positiveProgress
                    },
                containerColor = Color.Unspecified,
                contentColor = LocalContentColor.current
            ) {
                MainRoute.entries.forEachIndexed { i, it ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                painterResource(if (selected == it) it.selected else it.unselected),
                                contentDescription = it.name,
                            )
                        },
                        label = {
                            Text(it.name, Modifier.offset(y = -(2).dp))
                        },
                        selected = selected == it,
                        onClick = { onSelected(it) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = colorScheme.onPrimaryFixedVariant,
                            indicatorColor = colorScheme.primaryFixedDim
                        ),
                        modifier = Modifier.graphicsLayer {
                            val positiveProgress = sheetProgress.floatValue.coerceAtLeast(0f)
                            val transitionValue = maxOf(positiveProgress, animated.value)
                            translationX = transitionValue * -size.width * i / 2
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionSelectorFABMenu(
    modifier: Modifier = Modifier,
) = Box(modifier.fillMaxSize().safeDrawingPadding()) {
    val focusRequester = remember { FocusRequester() }
    val items = remember {
        listOf(
            "https://play-lh.googleusercontent.com/6am0i3walYwNLc08QOOhRJttQENNGkhlKajXSERf3JnPVRQczIyxw2w3DxeMRTOSdsY" to "Youtube",
            "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN" to "Spotify",
            "https://play-lh.googleusercontent.com/zD8UA5CRdiPzbvTwGKtzR4KjQpxqEK6X0tGDpzEaOo0xPEvG6HUiC_0qkpTfzpuMTqU" to "Youtube Music",
        )
    }
    val selected = items[1]
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    FloatingActionButtonMenu(
        expanded = fabMenuExpanded,
        modifier = Modifier.align(Alignment.BottomEnd),
        horizontalAlignment = Alignment.End,
        button = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    if (fabMenuExpanded) TooltipAnchorPosition.Start
                    else TooltipAnchorPosition.Above
                ),
                tooltip = { PlainTooltip { Text("Extensions menu") } },
                state = rememberTooltipState(),
            ) {
                ToggleFloatingActionButton(
                    modifier = Modifier.semantics {
                        traversalIndex = -1f
                        stateDescription =
                            if (fabMenuExpanded) "Expanded" else "Collapsed"
                        contentDescription = "Extensions menu"
                    }.animateFloatingActionButton(
                        visible = true,
                        alignment = Alignment.BottomEnd,
                    ).focusRequester(focusRequester),
                    containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                        initialColor = colorScheme.tertiaryContainer,
                        finalColor = colorScheme.tertiary,
                    ),
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                ) {
                    val icon by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Res.drawable.ic_close
                            else Res.drawable.ic_extension
                        }
                    }
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.animateIcon(
                            { checkedProgress },
                            iconColor(
                                initialColor = colorScheme.onTertiaryContainer,
                                finalColor = colorScheme.onTertiary,
                            )
                        )
                    )
                }
            }
        },
    ) {
        items.forEachIndexed { _, item ->
            FloatingActionButtonMenuItem(
                containerColor = if (item == selected) colorScheme.tertiary
                else colorScheme.surfaceContainerHighest,
                onClick = { fabMenuExpanded = false },
                icon = { },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BetterImage(
                            { item.first },
                            contentDescription = item.second,
                            Modifier.offset(-(12).dp).size(32.dp).clip(Circle.toShape())
                        )
                        Text(text = item.second)
                    }
                },
            )
        }
        FloatingActionButtonMenuItem(
            containerColor = colorScheme.surfaceContainerHighest,
            onClick = { fabMenuExpanded = false },
            icon = { },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_more_vert),
                        contentDescription = null,
                        Modifier.offset(-(8).dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("More")
                }
            },
        )
    }
}