package dev.brahmkshatriya.echo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.crossfade.CrossfadePlugin
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin
import dev.brahmkshatriya.echo.platform.IdiosyncrasyPlugin
import echo.app.generated.resources.Res
import echo.app.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

@Composable
fun BetterImage(
    model: () -> Any?,
    contentDescription: String?,
    modifier: Modifier,
) {
    if (!LocalInspectionMode.current) LandscapistImage(
        model,
        modifier,
        component = rememberImageComponent {
            +IdiosyncrasyPlugin()
            +ShimmerPlugin(Shimmer.Flash(
                baseColor = Color.Transparent,
                highlightColor = colorScheme.surfaceColorAtElevation(4.dp),
            ))
            +CrossfadePlugin()
        },
        imageOptions = ImageOptions(contentDescription = contentDescription),
        failure = {
            println("LANDSCAPIST FAILED: ${it.reason?.stackTraceToString()}")
        }
    ) else Image(painterResource(Res.drawable.compose_multiplatform), "", modifier)
}