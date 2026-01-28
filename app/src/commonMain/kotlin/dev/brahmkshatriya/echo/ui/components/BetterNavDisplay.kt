package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

val LocalMainBackStack = compositionLocalOf<NavBackStack<NavKey>?> { null }

const val SCALE = 0.98f
const val TWEEN_RATIO = 0.35f
val tweenIn = tween<Float>(TIME_MS) {
    if (it < TWEEN_RATIO) 0f else (it - TWEEN_RATIO) / (1f - TWEEN_RATIO)
}

val tweenOut = tween<Float>(TIME_MS) {
    if (it < TWEEN_RATIO) it / TWEEN_RATIO else 1f
}

val transitionSpec = run {
    val enter = fadeIn(tweenIn) + scaleIn(tween(TIME_MS) {
        springCubicBezierEasing.transform(tweenIn.easing.transform(it))
    }, SCALE)
    val exit = fadeOut(tweenOut) + scaleOut(tween(TIME_MS) {
        springCubicBezierEasing.transform(tweenOut.easing.transform(it))
    }, SCALE)
    enter togetherWith exit
}

@Composable
fun BetterNavDisplay(
    backStack: NavBackStack<NavKey>,
    isBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    entryProviderBuilder: EntryProviderScope<NavKey>.() -> Unit,
) {
    require(backStack.isNotEmpty()) { "NavDisplay entries cannot be empty" }
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider(builder = entryProviderBuilder),
    )
    val sceneState = rememberSceneState(entries, SinglePaneSceneStrategy()) {
        backStack.removeLastOrNull()
    }
    val scene = sceneState.currentScene
    val navigationEventState = rememberNavigationEventState(
        SceneInfo(scene),
        sceneState.previousScenes.map { SceneInfo(it) }
    )

    if (LocalNavigationEventDispatcherOwner.current != null) NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = isBackEnabled && scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            repeat(entries.size - scene.previousEntries.size) { backStack.removeLastOrNull() }
        },
    )
    CompositionLocalProvider(LocalMainBackStack provides backStack) {
        NavDisplay(
            sceneState,
            navigationEventState,
            modifier,
            transitionSpec = { transitionSpec },
            popTransitionSpec = { transitionSpec },
            predictivePopTransitionSpec = { transitionSpec }
        )
    }
}