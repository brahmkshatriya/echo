@file:OptIn(ExperimentalMaterial3Api::class)

package dev.brahmkshatriya.echo.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
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
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import dev.brahmkshatriya.echo.ui.components.BetterImage
import dev.brahmkshatriya.echo.ui.components.simpleTween
import dev.brahmkshatriya.echo.ui.theme.Primary
import dev.brahmkshatriya.echo.ui.theme.animateBounds
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_close
import echo.app.generated.resources.ic_keyboard_arrow_down
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun PlayerItem(image: String, i: Int) = Box(Modifier.fillMaxSize()) {
    val paletteState = rememberPaletteState()
    val scheme = paletteState.value?.let {
        rememberDynamicMaterialThemeState(
            isDark = isSystemInDarkTheme(),
            style = PaletteStyle.Rainbow,
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
            primary = (it.vibrantSwatch ?: it.dominantSwatch ?: it.lightVibrantSwatch)?.color
                ?: Primary,
            secondary = (it.mutedSwatch ?: it.lightMutedSwatch)?.color,
            tertiary = (it.darkVibrantSwatch ?: it.mutedSwatch)?.color,
        ).colorScheme
    } ?: colorScheme
    MaterialExpressiveTheme(animateColorScheme(scheme)) {
        SongPlayerItem(paletteState, image, i)
    }
}

@Preview
@Composable
fun SongPlayerItem(
    paletteState: MutableState<Palette?>, image: String, i: Int
) = CompositionLocalProvider(
    LocalContentColor provides colorScheme.onPrimary
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()
    val peekHeight = playerSheet?.peekHeight ?: 72.dp

    val layoutDirection = LocalLayoutDirection.current
    val startPadding = playerPadding.calculateStartPadding(layoutDirection)
    val endPadding = playerPadding.calculateEndPadding(layoutDirection)
    val animatedStart = animateDpAsState(startPadding + 12.dp, simpleTween())
    val animatedEnd = animateDpAsState(endPadding + 12.dp, simpleTween())

    Box(Modifier.fillMaxSize().graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        val backProgress = playerSheet?.backProgressState?.floatValue ?: 0f
        clip = true
        shape = ClippedShape(
            peekHeight - 8.dp,
            positiveProgress,
            backProgress,
            animatedStart.value,
            animatedEnd.value
        )
    }.background(colorScheme.primary))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .animateBounds()
            .wrapContentHeight()
            .graphicsLayer {
                val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                alpha = 1 - positiveProgress
                translationY = -positiveProgress * size.height
            }
            .padding(playerPadding)
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { scope.launch { sheetState?.expand() } }
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.padding(start = 4.dp).size(48.dp))
        Column(Modifier.weight(1f)) {
            val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
            Text("Song $i", fontWeight = FontWeight(600), style = mergedStyle)
            Text("Artist $i", style = mergedStyle)
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
    val animatedTargetX = animateDpAsState(
        playerPadding.calculateStartPadding(layoutDirection) + 20.dp,
        simpleTween()
    )
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
                    val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                    val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                    val offset = 1 - sheetProgress.coerceIn(0f, 1f)
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
        val horizontalPadding = remember { 16.dp }
        val verticalPadding = remember { 8.dp }
        val maxSize = remember { 320.dp }
        BetterImage(
            { image },
            "Song $i",
            Modifier
                .padding(vertical = verticalPadding, horizontal = horizontalPadding)
                .widthIn(max = maxSize)
                .heightIn(max = maxSize)
                .aspectRatio(1f)
                .fillMaxSize()
                .graphicsLayer {
                    val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                    val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                    val offset = 1 - sheetProgress.coerceIn(0f, 1f)

                    val targetX = animatedTargetX.value
                    val targetY = 8.dp
                    val targetSize = 48.dp

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
                .background(colorScheme.primaryFixed),
            PalettePlugin { paletteState.value = it }
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