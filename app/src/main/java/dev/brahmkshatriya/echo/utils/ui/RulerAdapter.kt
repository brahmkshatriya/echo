package dev.brahmkshatriya.echo.utils.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.databinding.ItemRulerBinding
import dev.brahmkshatriya.echo.databinding.ItemRulerEmptyBinding
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx

class RulerAdapter<T>(
    private val listener: Listener<T>
) : RecyclerView.Adapter<RulerAdapter.ViewHolder>() {

    interface Listener<T> {
        fun intervalText(value: T): String
        fun onSelectItem(value: T)

        val selectedColor get() = androidx.appcompat.R.attr.colorPrimary
        val unselectedColor get() = R.attr.colorOnSurface
    }

    sealed class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        open fun bind(position: Int) {}

        class Empty(
            parent: ViewGroup, binding: ItemRulerEmptyBinding = ItemRulerEmptyBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ) : ViewHolder(binding.root) {
            init {
                parent.doOnLayout {
                    binding.root.updateLayoutParams {
                        val itemWidth = 24.dpToPx(it.context)
                        width = (it.width - itemWidth) / 2
                    }
                }
            }
        }

        class Item<T>(
            parent: ViewGroup,
            private val getItem: (Int) -> Pair<T, Boolean>?,
            private val listener: Listener<T>,
            private val binding: ItemRulerBinding = ItemRulerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ) : ViewHolder(binding.root) {
            override fun bind(position: Int) {
                val (value, showText) = getItem(position) ?: return
                binding.rulerText.text = if (showText) listener.intervalText(value) else ""
                unselect()
            }

            fun select() {
                binding.rulerCard.setCardBackgroundColor(
                    MaterialColors.getColor(itemView, listener.selectedColor, 0)
                )
            }

            fun unselect() {
                binding.rulerCard.setCardBackgroundColor(
                    MaterialColors.getColor(itemView, listener.unselectedColor, 0)
                )
            }
        }
    }

    override fun getItemViewType(position: Int) = when (position) {
        0, itemCount - 1 -> 0
        else -> 1
    }

    private var currentList = listOf<Pair<T, Boolean>?>(null, null)
    private fun getItem(position: Int): Pair<T, Boolean>? {
        return currentList.getOrNull(position)
    }

    override fun getItemCount() = currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            0 -> ViewHolder.Empty(parent)
            else -> ViewHolder.Item(parent, ::getItem, listener)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    private var selectedIndex: Int = -1
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<Pair<T, Boolean>>, selected: T) {
        currentList = buildList {
            add(null)
            addAll(list)
            add(null)
        }
        val index = list.indexOfFirst { it.first == selected }
        selectedIndex = index + 1
        notifyDataSetChanged()
    }

    private var recyclerView: RecyclerView? = null
    private var selectedVH: ViewHolder.Item<*>? = null
    private fun select() {
        val (vh, newIndex) = getMiddleItem() ?: return
        if (vh == selectedVH) return
        selectedIndex = newIndex
        selectedVH?.unselect()
        vh.select()
        selectedVH = vh
        vh.itemView.run {
            isHapticFeedbackEnabled = true
            performHapticFeedback(HapticFeedbackConstantsCompat.CLOCK_TICK)
        }
        listener.onSelectItem(getItem(newIndex)!!.first)
    }

    private val recyclerListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            select()
        }
    }

    private fun scrollToMiddle(after: () -> Unit = {}) {
        recyclerView?.run {
            val manager = layoutManager as LinearLayoutManager
            doOnLayout {
                val itemWidth = 24.dpToPx(context)
                val pad = (width - itemWidth) / 2
                manager.scrollToPositionWithOffset(selectedIndex, pad)
                post {
                    after()
                }
            }
        }
    }

    private val snapHelper = PagerSnapHelper()
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        snapHelper.attachToRecyclerView(recyclerView)
        scrollToMiddle {
            select()
            recyclerView.addOnScrollListener(recyclerListener)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView?.removeOnScrollListener(recyclerListener)
        snapHelper.attachToRecyclerView(null)
        this.recyclerView = null
    }

    private fun getMiddleItem() = recyclerView?.run {
        val middle = width * .5f
        val layoutManager = layoutManager as LinearLayoutManager

        val firstVisibleIndex = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleIndex = layoutManager.findLastVisibleItemPosition()
        val visibleIndexes = firstVisibleIndex..lastVisibleIndex

        visibleIndexes.forEach {
            if (it == 0 || it == itemCount - 1) return@forEach
            val vh = findViewHolderForLayoutPosition(it) ?: return@forEach
            val location = IntArray(2)
            vh.itemView.getLocationOnScreen(location)
            val x = location[0]
            val halfWidth = vh.itemView.run { width + marginLeft + marginRight } * .5f
            val rightSide = x + halfWidth
            val leftSide = x - halfWidth
            val isInMiddle = middle in leftSide..rightSide
            if (isInMiddle) {
                return@run vh as ViewHolder.Item<*> to it
            }
        }
        null
    }
}