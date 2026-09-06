package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import echo.app.generated.resources.GoogleSansFlex
import echo.app.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
fun PreloadComposeResources() {
    val expandedTitleVariation = remember {
        FontVariation.Settings(
            FontVariation.Setting("wght", 800f),
            FontVariation.Setting("wdth", 125f),
        )
    }
    val collapsedTitleVariation = remember {
        FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("wdth", 100f),
        )
    }

    Font(
        Res.font.GoogleSansFlex,
        weight = FontWeight.ExtraBold,
        variationSettings = expandedTitleVariation,
    )
    Font(
        Res.font.GoogleSansFlex,
        weight = FontWeight.Medium,
        variationSettings = collapsedTitleVariation,
    )
}
