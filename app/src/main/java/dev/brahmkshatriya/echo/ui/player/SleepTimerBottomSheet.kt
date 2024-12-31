package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.doOnLayout
import androidx.fragment.app.activityViewModels
import androidx.media3.common.C
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogSleepTimerBinding
import dev.brahmkshatriya.echo.databinding.ItemRulerBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import java.util.Calendar

class SleepTimerBottomSheet : BottomSheetDialogFragment() {
    var binding by autoCleared<DialogSleepTimerBinding>()
    private val viewModel by activityViewModels<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val last = viewModel.settings.getInt("sleep_timer", 5)
        binding.sleepTimerRecycler.apply {
            val adapter = Adapter()
            val snapHelper = PagerSnapHelper()
            val layoutManager = layoutManager as LinearLayoutManager
            this.adapter = adapter
            snapHelper.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    post { selectMiddleItem() }
                }
            })
            doOnLayout {
                val itemWidth = 24.dpToPx(requireContext())
                val padding = (width - itemWidth) / 2
                adapter.padding = padding / itemWidth
                adapter.notifyDataSetChanged()
                post {
                    layoutManager.scrollToPositionWithOffset(
                        adapter.getPositionFromTime(last), padding
                    )
                    post { selectMiddleItem() }
                }
            }
        }

        binding.okay.setOnClickListener { saveAndDismiss(rulerTime) }
        binding.endOfTrack.setOnClickListener { setTimerAndDismiss(Long.MAX_VALUE) }
        binding.min15.setOnClickListener { saveAndDismiss(15) }
        binding.min30.setOnClickListener { saveAndDismiss(30) }
        binding.min45.setOnClickListener { saveAndDismiss(45) }
        binding.hr1.setOnClickListener { saveAndDismiss(60) }
        binding.hr2.setOnClickListener { saveAndDismiss(120) }

        binding.topAppBar.setNavigationOnClickListener { setTimerAndDismiss(0) }
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.schedule -> {
                    val is24Hour = DateFormat.is24HourFormat(context)
                    val picker = MaterialTimePicker.Builder()
                        .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                        .setTitleText(getString(R.string.sleep_on_specific_time))
                        .build()
                    picker.addOnCancelListener { picker.dismiss() }
                    picker.addOnPositiveButtonClickListener {
                        val now = Calendar.getInstance()
                        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                        val pickerMin = picker.hour * 60 + picker.minute
                        val time = if (pickerMin > nowMin) pickerMin - nowMin
                        else 1440 - nowMin + pickerMin
                        setTimerAndDismiss(time * 60L * 1000)
                    }
                    picker.show(childFragmentManager, null)
                    true
                }

                else -> false
            }
        }
    }

    private var timer = 0L
    private fun setTimerAndDismiss(ms: Long) {
        timer = ms
        dismiss()
    }

    private fun saveAndDismiss(mins: Int) {
        viewModel.settings.edit { putInt("sleep_timer", mins) }
        val ms = mins * 60L * 1000
        setTimerAndDismiss(ms)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.setSleepTimer(timer)
        val message = when (timer) {
            0L -> getString(R.string.sleep_timer_canceled)
            Long.MAX_VALUE -> {
                val until = viewModel.browser.value?.run {
                    (duration.takeIf { it != C.TIME_UNSET } ?: 0) - currentPosition
                } ?: 0
                getString(R.string.sleep_timer_set_for, requireContext().createString(until))
            }

            else -> getString(R.string.sleep_timer_set_for, requireContext().createString(timer))
        }
        createSnack(message)
    }

    private var rulerTime = 5
    private fun selectMiddleItem() = binding.sleepTimerRecycler.run {
        val screenWidth = resources.displayMetrics.widthPixels
        val layoutManager = layoutManager as LinearLayoutManager
        val adapter = adapter as Adapter

        fun selected(vh: Adapter.ViewHolder, pos: Int) {
            adapter.selectItem(vh)
            rulerTime = vh.time(pos)
            binding.sleepTimerValue.text = context.createString(rulerTime * 60L * 1000)
        }

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
                selected(vh as Adapter.ViewHolder, it)
                return
            }
        }
    }

    companion object {
        const val MAX = 360
        const val MIN = 5
        const val STEPS = 5
        const val INTERVAL = 15

        private fun Context.createString(ms: Long): String {
            val minutes = ms / 1000 / 60
            val hrs = minutes / 60
            val min = minutes % 60
            val str = StringBuilder()
            if (hrs > 0) str.append(
                resources.getQuantityString(R.plurals.number_hour, hrs.toInt(), hrs)
            ).append(if (min > 0) " " else "")
            if (min > 0) str.append(
                resources.getQuantityString(R.plurals.number_min, min.toInt(), min)
            )
            return str.toString()
        }
    }

    class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemRulerBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind()
        }

        var padding = 0
        override fun getItemCount() = (MAX - MIN) / STEPS + (padding + 1) * 2 + 1
        fun getPositionFromTime(time: Int) = (time - MIN) / STEPS + padding + 1

        inner class ViewHolder(val binding: ItemRulerBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun time(pos: Int) = MIN + (pos - padding - 1) * STEPS
            fun bind() {
                val time = time(bindingAdapterPosition)
                binding.root.visibility =
                    if (time < MIN || time > MAX) View.INVISIBLE else View.VISIBLE
                val shouldShowValue = time == MIN || time % INTERVAL == 0
                binding.rulerText.text = if (shouldShowValue) time.toString() else ""
            }

            private val selectedColor = MaterialColors.getColor(
                itemView, com.google.android.material.R.attr.colorPrimary, 0
            )

            private val unselectedColor = MaterialColors.getColor(
                itemView, com.google.android.material.R.attr.colorOnSurface, 0
            )

            fun select() {
                binding.rulerCard.setCardBackgroundColor(selectedColor)
            }

            fun unselect() {
                binding.rulerCard.setCardBackgroundColor(unselectedColor)
            }
        }

        private var selectedVh: ViewHolder? = null
        fun selectItem(vh: ViewHolder) {
            selectedVh?.unselect()
            vh.select()
            selectedVh = vh
        }


    }
}