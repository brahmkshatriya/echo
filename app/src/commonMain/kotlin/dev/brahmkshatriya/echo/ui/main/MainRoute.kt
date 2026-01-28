package dev.brahmkshatriya.echo.ui.main

import androidx.compose.runtime.Composable
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_bookmark_filled
import echo.app.generated.resources.ic_bookmark_outline
import echo.app.generated.resources.ic_home_filled
import echo.app.generated.resources.ic_home_outline
import echo.app.generated.resources.ic_search_filled
import echo.app.generated.resources.ic_search_outline
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource

@Serializable
enum class MainRoute(
//    val id: @StringRes Int,
    val selected: DrawableResource,
    val unselected: DrawableResource,
    val content: @Composable () -> Unit
) {
    Home(Res.drawable.ic_home_filled, Res.drawable.ic_home_outline, ::Home),
    Search(Res.drawable.ic_search_filled, Res.drawable.ic_search_outline, ::Search),
    Library(Res.drawable.ic_bookmark_filled,Res.drawable.ic_bookmark_outline, ::Library),
}