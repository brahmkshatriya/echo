package dev.brahmkshatriya.echo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class CardParams(
    val modifier: Modifier,
    val key: Any?,
    val contentType: Any?,
    val colors: CardColors?,
    val elevation: CardElevation?,
    val border: BorderStroke?,
    val content: @Composable () -> Unit,
)

class MaterialGroupScope internal constructor(
    private val radius: Dp,
    private val verticalPadding: Dp,
    private val lazyListScope: LazyListScope,
) {
    private val zero = 0.dp
    private val items = mutableListOf<CardParams>()

    fun card(
        modifier: Modifier = Modifier,
        key: Any? = null,
        contentType: Any? = null,
        colors: CardColors? = null,
        elevation: CardElevation? = null,
        border: BorderStroke? = null,
        content: @Composable () -> Unit,
    ) {
        items += CardParams(modifier, key, contentType, colors, elevation, border, content)
    }

    internal fun emit() {
        val lastIndex = items.lastIndex
        items.forEachIndexed { index, params ->
            lazyListScope.item(params.key, params.contentType) {
                val isTop = index == 0
                val isBottom = index == lastIndex
                val shape = RoundedCornerShape(
                    topStart = if (isTop) radius else zero,
                    topEnd = if (isTop) radius else zero,
                    bottomStart = if (isBottom) radius else zero,
                    bottomEnd = if (isBottom) radius else zero
                )

                Card(
                    modifier = params.modifier.padding(
                        top = if (isTop) verticalPadding else zero,
                        bottom = if (isBottom) verticalPadding else zero
                    ),
                    colors = params.colors ?: CardDefaults.cardColors(),
                    elevation = params.elevation ?: CardDefaults.cardElevation(),
                    border = params.border,
                    shape = shape
                ) {
                    params.content()
                }
            }
        }
    }
}


fun LazyListScope.materialGroup(
    roundedCornerRadius: Dp = 16.dp,
    verticalPadding: Dp = 8.dp,
    content: MaterialGroupScope.() -> Unit,
) {
    val scope = MaterialGroupScope(
        radius = roundedCornerRadius,
        verticalPadding = verticalPadding,
        lazyListScope = this
    )

    scope.content()
    scope.emit()
}