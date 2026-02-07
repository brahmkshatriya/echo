package dev.brahmkshatriya.echo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.brahmkshatriya.echo.platform.getPlatform
import dev.brahmkshatriya.echo.ui.components.BetterNavDisplay
import dev.brahmkshatriya.echo.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.ui.components.expandingButton
import dev.brahmkshatriya.echo.ui.main.ExtensionSelectorFABMenu
import dev.brahmkshatriya.echo.ui.main.MainRoute
import dev.brahmkshatriya.echo.ui.main.MainSideNavigation
import dev.brahmkshatriya.echo.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.ui.player.PlayerBottomSheet
import dev.brahmkshatriya.echo.ui.theme.EchoTheme
import dev.brahmkshatriya.echo.ui.theme.LocalSurfaceColor
import echo.app.generated.resources.Res
import echo.app.generated.resources.compose_multiplatform
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.painterResource

@Serializable
data class Main(val route: MainRoute) : NavKey

@Serializable
data class Media(val id: String) : NavKey

private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Main::class, Main.serializer())
            subclass(Media::class, Media.serializer())
        }
    }
}

@Preview
@Composable
fun App() = EchoTheme {
    var sheetProgress by remember { mutableFloatStateOf(0f) }
    var peekHeight by remember { mutableStateOf(0.dp) }
    val startPadding = remember { mutableStateOf(0.dp) }
    val bottomPadding = remember { mutableStateOf(0.dp) }
    val backStack = rememberNavBackStack(
        config, Main(MainRoute.Home)
    )
    PlayerBottomSheet(startPadding.value, bottomPadding.value) {
        sheetProgress = LocalPlayerSheet.current?.sheetProgressState?.value ?: 0f
        peekHeight = LocalPlayerSheet.current?.peekHeight?.value ?: 0.dp
        val isSheetHidden = sheetProgress < -0.8f
        val sheetPadding = if (isSheetHidden) 0.dp else peekHeight
        LookaheadScope {
            val modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = startPadding.value,
                    bottom = bottomPadding.value
                )
                .padding(bottom = sheetPadding)
                .animateBounds(this)
            BetterNavDisplay(
                backStack,
                LocalPlayerSheet.current?.isExpanded?.value?.not() ?: true,
                modifier
            ) {
                entry<Main> {
                    it.route.content()
                }

                entry<Media> {
                    Test(it.toString())
                }
            }
            AnimatedVisibility(backStack.size == 1, modifier, fadeIn(), fadeOut()) {
                ExtensionSelectorFABMenu()
            }
        }
    }

    MainSideNavigation(
        backStack.size == 1,
        backStack.size == 2,
        peekHeight,
        sheetProgress,
        (backStack.last() as? Main)?.route,
        bottomPadding,
        startPadding
    ) {
        if (backStack.size == 1) backStack[0] = Main(it)
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