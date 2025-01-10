package dev.brahmkshatriya.echo.utils.ui.custom

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.databinding.ItemRulerBinding
import dev.brahmkshatriya.echo.utils.ui.dpToPx

@SuppressLint("NotifyDataSetChanged")
class RulerAdapter(
    private val recyclerView: RecyclerView,
    private val rangeWithIntervals: List<Pair<Int, Boolean>>,
    private val default: Int,
    private val intervalText: (Int) -> String,
    private val onSelected: (Int) -> Unit = {}
) : RecyclerView.Adapter<RulerAdapter.ViewHolder>() {
    private val range = rangeWithIntervals.map { it.first }

    init {
        recyclerView.apply {
            val layoutManager = layoutManager as LinearLayoutManager
            adapter = this@RulerAdapter
            doOnLayout {
                val itemWidth = 24.dpToPx(context)
                val pad = (width - itemWidth) / 2
                padding = pad / itemWidth
                notifyDataSetChanged()
                post {
                    layoutManager.scrollToPositionWithOffset(getPositionFromValue(default), pad)
                    post {
                        selectMiddleItem()
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                post { selectMiddleItem() }
                            }
                        })
                    }
                }
            }
        }
    }

    private fun selectMiddleItem() = recyclerView.run {
        val screenWidth = resources.displayMetrics.widthPixels
        val layoutManager = layoutManager as LinearLayoutManager

        val firstVisibleIndex = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleIndex = layoutManager.findLastVisibleItemPosition()
        val visibleIndexes = firstVisibleIndex..lastVisibleIndex

        visibleIndexes.forEach {
            val vh = findViewHolderForLayoutPosition(it) ?: return@forEach
            val location = IntArray(2)
            vh.itemView.getLocationOnScreen(location)
            val x = location[0]
            val halfWidth = vh.itemView.width * .5
            val rightSide = x + halfWidth
            val leftSide = x - halfWidth
            val isInMiddle = screenWidth * .5 in leftSide..rightSide
            if (isInMiddle) {
                selectItem(vh as ViewHolder, it)
                return
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRulerBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    var padding = 0
    override fun getItemCount() = range.size + (padding + 1) * 2 - 1
    private fun getPositionFromValue(value: Int) = range.indexOf(value) + padding + 1

    inner class ViewHolder(val binding: ItemRulerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private fun actualValue(pos: Int) = rangeWithIntervals.getOrNull(pos - padding - 1)
            ?: (null to false)

        fun bind() {
            val (value, showText) = actualValue(bindingAdapterPosition)
            binding.root.visibility = if (value == null) View.INVISIBLE else View.VISIBLE
            binding.rulerText.text = if (showText && value != null) intervalText(value) else ""
        }

        private val selectedColor = MaterialColors.getColor(
            itemView, R.attr.colorPrimary, 0
        )

        private val unselectedColor = MaterialColors.getColor(
            itemView, R.attr.colorOnSurface, 0
        )

        fun select() {
            binding.rulerCard.setCardBackgroundColor(selectedColor)
        }

        fun unselect() {
            binding.rulerCard.setCardBackgroundColor(unselectedColor)
        }
    }

    private var selectedVh: ViewHolder? = null
    private var last: Int? = null
    private fun selectItem(vh: ViewHolder, pos: Int) {
        val value = range.getOrNull(pos - padding - 1)
        if (value == last) return
        last = value

        selectedVh?.unselect()
        vh.select()
        selectedVh = vh

        vh.itemView.run {
            isHapticFeedbackEnabled = true
            performHapticFeedback(HapticFeedbackConstantsCompat.CLOCK_TICK)
        }
        onSelected(value ?: return)
    }
}