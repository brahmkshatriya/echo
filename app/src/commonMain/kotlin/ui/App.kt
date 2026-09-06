package dev.brahmkshatriya.echo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.brahmkshatriya.echo.app.platform.PreloadComposeResources
import dev.brahmkshatriya.echo.app.platform.getPlatform
import dev.brahmkshatriya.echo.app.ui.components.BetterNavDisplay
import dev.brahmkshatriya.echo.app.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.app.ui.components.expandingButton
import dev.brahmkshatriya.echo.app.ui.components.rememberBetterSheet
import dev.brahmkshatriya.echo.app.ui.main.ExtensionSelectorFABMenu
import dev.brahmkshatriya.echo.app.ui.main.MainRoute
import dev.brahmkshatriya.echo.app.ui.main.MainSideNavigation
import dev.brahmkshatriya.echo.app.ui.player.LocalInitialPlayerSheetValue
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.PlayerBottomSheet
import dev.brahmkshatriya.echo.app.ui.theme.EchoTheme
import dev.brahmkshatriya.echo.app.ui.theme.LocalSurfaceColor
import echo.app.generated.resources.Res
import echo.app.generated.resources.compose_multiplatform
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.painterResource


@Serializable sealed interface AppNavKey: NavKey
@Serializable data class Main(val route: MainRoute) : AppNavKey
@Serializable data class Media(val id: String) : AppNavKey

@OptIn(ExperimentalSerializationApi::class)
private val module = SerializersModule {
    polymorphic(NavKey::class) { subclassesOfSealed<AppNavKey>() }
}
private val config = SavedStateConfiguration { serializersModule = module }

@Composable
fun App() = EchoTheme {
    PreloadComposeResources()
    val initialSheetValue = LocalInitialPlayerSheetValue.current
    val betterSheet = rememberBetterSheet(80.dp, initialSheetValue)
    val backStack = rememberNavBackStack(config, Main(MainRoute.Home))
    val isNavigationVisible = backStack.size == 1
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val showNavigationBar = windowWidth.isUnspecified || windowWidth < 560.dp
    val targetStartPadding = if (isNavigationVisible && !showNavigationBar) 72.dp else 0.dp
    val targetBottomPadding = if (isNavigationVisible && showNavigationBar) 64.dp else 0.dp
    val animatedStartPadding by animateDpAsState(
        targetStartPadding,
        tween(),
        label = "Content start padding",
    )
    val animatedBottomPadding by animateDpAsState(
        targetBottomPadding,
        tween(),
        label = "Content bottom padding",
    )
    val sheetPaddingState = remember { mutableStateOf(0.dp) }
    LaunchedEffect(betterSheet) {
        snapshotFlow { betterSheet.progressState.floatValue < -0.8f }.collect {
            sheetPaddingState.value = if (it) 0.dp
            else (betterSheet.peekHeight - 8.dp).coerceAtLeast(0.dp)
        }
    }
    val animatedSheetPadding by animateDpAsState(
        sheetPaddingState.value,
        tween(),
        label = "Content sheet padding",
    )

    PlayerBottomSheet(betterSheet, targetStartPadding, targetBottomPadding) {
        val modifier = Modifier
            .fillMaxSize()
            .padding(
                start = animatedStartPadding.coerceAtLeast(0.dp),
                bottom = (animatedBottomPadding + animatedSheetPadding)
                    .coerceAtLeast(0.dp),
            )
        val isExpanded = LocalPlayerSheet.current?.isExpandedState?.value ?: false
        BetterNavDisplay(backStack, !isExpanded, modifier) {
            entry<Main> { it.route.content() }
            entry<Media> { Test(it.toString()) }
        }
        AnimatedVisibility(isNavigationVisible, modifier, fadeIn(), fadeOut()) {
            ExtensionSelectorFABMenu()
        }
    }

    MainSideNavigation(
        isVisible = isNavigationVisible,
        wasVisible = backStack.size == 2,
        showNavigationBar = showNavigationBar,
        sheetProgress = betterSheet.progressState,
        selected = (backStack.last() as? Main)?.route,
    ) {
        if (isNavigationVisible) backStack[0] = Main(it)
    }
}

@Composable
fun ExpandingButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.expandingButton(interactionSource),
        content = content
    )
}

@Composable
fun Test(string: String) {
    Column(
        modifier = Modifier.fillMaxSize()
            .safeDrawingPadding()
            .padding(8.dp)
            .background(
                LocalSurfaceColor.current,
                shapes.large,
            )
            .scrollable(rememberScrollableState { it }, Orientation.Vertical)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val scope = rememberCoroutineScope()
        val playerUi = LocalPlayerSheet.current
        var showContent by rememberSaveable { mutableStateOf(false) }
        val backStack = LocalMainBackStack.current
        ExpandingButton(onClick = {
            backStack?.let {
                if (it.size == 1) return@let
                it.removeLastOrNull()
            }
            scope.launch { playerUi?.sheetState?.show() }
        }) {
            Text("Back")
        }
        ExpandingButton(onClick = {
            backStack?.add(Media(backStack.size.toString()))
        }) {
            Text("Next")
        }
        ExpandingButton(onClick = {
            showContent = !showContent
            scope.launch { playerUi?.sheetState?.show() }
        }) {
            Text(string)
        }
        AnimatedVisibility(
            showContent,
            enter = fadeIn() + expandVertically(motionScheme.defaultSpatialSpec()),
            exit = fadeOut() + shrinkVertically(motionScheme.defaultSpatialSpec())
        ) {
            val greeting = remember { "Hello from ${getPlatform().name}" }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(Res.drawable.compose_multiplatform), null)
                Text("Compose: $greeting")
            }
        }
    }
}
