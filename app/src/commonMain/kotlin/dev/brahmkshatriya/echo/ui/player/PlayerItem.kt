@file:OptIn(ExperimentalMaterial3Api::class)

package dev.brahmkshatriya.echo.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.ui.components.BetterImage
import dev.brahmkshatriya.echo.ui.components.simpleTween
import dev.brahmkshatriya.echo.ui.theme.animateBounds
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_close
import echo.app.generated.resources.ic_keyboard_arrow_down
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun BoxScope.PlayerItem() {
    SongPlayerItem()
}

@Preview
@Composable
fun SongPlayerItem() = CompositionLocalProvider(
    LocalContentColor provides colorScheme.onPrimary
) {
    val playerUi = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val sheetState = playerUi?.sheetState
    val sheetProgress = playerUi?.sheetProgressState?.value ?: 0f
    val positiveProgress = sheetProgress.coerceAtLeast(0f)
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer {
            alpha = 1 - positiveProgress
            translationY = -positiveProgress * size.height
        }.padding(playerPadding).animateBounds().clickable {
            scope.launch { sheetState?.expand() }
        }.clip(RoundedCornerShape(16.dp)), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.padding(start = 8.dp).size(48.dp))
        Column(Modifier.weight(1f)) {
            val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
            Text("Song", fontWeight = FontWeight(600), style = mergedStyle)
            Text("Artist", style = mergedStyle)
        }
        IconButton(
            onClick = { scope.launch { sheetState?.hide() } }, shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                painterResource(Res.drawable.ic_close), contentDescription = "Close Player"
            )
        }
    }
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val targetSize = 48.dp
    val animatedTargetX = animateDpAsState(
        playerPadding.calculateStartPadding(layoutDirection) + 8.dp,
        simpleTween()
    )
    val targetX by animatedTargetX
    val targetY = 0.dp
    val offset = 1 - sheetProgress.coerceIn(0f, 1f)
    val widthState = remember { mutableIntStateOf(0) }
    Column(Modifier.onSizeChanged {
        widthState.intValue = it.width
    }) {
        val topBarHeight = remember { mutableIntStateOf(0) }
        Row(
            Modifier.padding(horizontal = 8.dp)
                .onSizeChanged {
                    topBarHeight.intValue = it.height
                }
                .padding(top = safePadding.calculateTopPadding())
                .graphicsLayer {
                    alpha = if (positiveProgress > 0.75f) (positiveProgress - 0.75f) * 4 else 0f
                    translationY = offset * size.height
                }
        ) {
            IconButton(
                onClick = {
                    scope.launch { sheetState?.show() }
                }, shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = "Minimize Player"
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { },
                shapes = IconButtonDefaults.shapes()
            ) {
                BetterImage(
                    model = { "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN" },
                    "Spotify",
                    modifier = Modifier.padding(4.dp).clip(Circle.toShape())
                )
            }
        }
        val horizontalPadding = 16.dp
        val verticalPadding = 8.dp
        Box(
            Modifier
                .padding(vertical = verticalPadding, horizontal = horizontalPadding)
                .widthIn(max = 320.dp)
                .heightIn(max = 320.dp)
                .aspectRatio(1f)
                .fillMaxSize()
                .graphicsLayer {
                    val targetScale = targetSize.toPx() / size.height
                    scaleX = 1 + (targetScale - 1) * offset
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(0f, 0f)
                    val center = (widthState.intValue - size.width) / 2f
                    translationX =
                        -horizontalPadding.toPx() + targetX.toPx() * offset + center * positiveProgress
                    val height = topBarHeight.intValue
                    translationY = (-verticalPadding.toPx() - height + targetY.toPx()) * offset
                    clip = true
                    shape = RoundedCornerShape((8 / scaleX).dp)
                }
                .background(colorScheme.primaryFixed)

        )
    }
}

//    val density = LocalDensity.current
//    val padding = WindowInsets.safeContent.asPaddingValues().calculateTopPadding()
//    val paddingTop = density.run { padding.toPx() }


//@Composable
//fun VideoPlayerItem() {
//
//}

//@Composable
//fun PodcastPlayerItem() {
//
//}