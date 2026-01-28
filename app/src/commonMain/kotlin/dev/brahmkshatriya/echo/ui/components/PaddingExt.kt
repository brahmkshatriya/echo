package dev.brahmkshatriya.echo.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

operator fun PaddingValues.plus(other: PaddingValues) = object : PaddingValues {
    fun add(block: PaddingValues.() -> Dp): Dp = block(this@plus) + block(other)

    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        add { calculateLeftPadding(layoutDirection) }

    override fun calculateTopPadding() =
        add { calculateTopPadding() }

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        add { calculateRightPadding(layoutDirection) }

    override fun calculateBottomPadding() =
        add { calculateBottomPadding() }
}

fun PaddingValues.minus(
    other: PaddingValues,
    minimum: PaddingValues = PaddingValues.Zero
) = object : PaddingValues {
    fun minus(block: PaddingValues.() -> Dp): Dp =
        maxOf(block(this@minus) - block(other), block(minimum))

    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        minus { calculateLeftPadding(layoutDirection) }

    override fun calculateTopPadding() =
        minus { calculateTopPadding() }

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        minus { calculateRightPadding(layoutDirection) }

    override fun calculateBottomPadding() =
        minus { calculateBottomPadding() }
}
