package dev.brahmkshatriya.echo.ui.common

import androidx.core.util.toKotlinPair
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.resolveStyledDimension
import kotlin.math.roundToInt

interface GridAdapter {
    val adapter: RecyclerView.Adapter<*>
    fun getSpanSize(position: Int, width: Int, count: Int): Int

    class Concat(
        vararg adapters: GridAdapter
    ) : GridAdapter {
        override val adapter = ConcatAdapter(adapters.map { it.adapter })
        private val getSpanSizeMap = adapters.mapIndexed { index, gridAdapter ->
            gridAdapter.adapter to gridAdapter::getSpanSize
        }.toMap()

        override fun getSpanSize(position: Int, width: Int, count: Int): Int {
            val (adapter, pos) = adapter.getWrappedAdapterAndPosition(position).toKotlinPair()
            val getSpanSize = getSpanSizeMap[adapter]
                ?: throw IllegalStateException("No span size function found for adapter: ${adapter.javaClass.name}")
            return getSpanSize(pos, width, count)
        }
    }

    companion object {
        fun configureGridLayout(
            recycler: RecyclerView, gridAdapter: GridAdapter, even: Boolean = true
        ) {
            val context = recycler.context
            val layoutManager = GridLayoutManager(context, 1)
            recycler.doOnLayout {
                val itemWidth = context.resolveStyledDimension(R.attr.itemCoverSize)
                val width = it.width - it.paddingLeft - it.paddingRight
                val calc = (width.toFloat() / (itemWidth + 8.dpToPx(context))).roundToInt()
                val count = if (calc > 1) calc - if (even) calc % 2 else 0 else 1
                layoutManager.spanCount = count
                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int) =
                        gridAdapter.getSpanSize(position, width, count)
                }
            }
            recycler.adapter = gridAdapter.adapter
            recycler.layoutManager = layoutManager
        }
    }
}