package dev.brahmkshatriya.echo.ui.player.sleep

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.media3.common.C
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogPlayerSleepTimerBinding
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.RulerAdapter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.Calendar

class SleepTimerBottomSheet : BottomSheetDialogFragment() {
    var binding by autoCleared<DialogPlayerSleepTimerBinding>()
    private val viewModel by activityViewModel<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPlayerSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val adapter by lazy {
        RulerAdapter(object : RulerAdapter.Listener<Int> {
            override fun intervalText(value: Int) = value.toString()
            override fun onSelectItem(value: Int) {
                rulerTime = value
                binding.sleepTimerValue.text = requireContext().createString(value * 60L * 1000)
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.sleepTimerRecycler.adapter = adapter
        val last = viewModel.settings.getInt("sleep_timer", 5)
        adapter.submitList(timeRangeWithIntervals, last)

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
                R.id.menu_schedule -> {
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
            0L -> getString(R.string.sleep_timer_cancelled)
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

    companion object {
        private const val MAX = 360
        private const val MIN = 5
        private const val STEPS = 5
        private const val INTERVAL = 15

        val timeRangeWithIntervals = 0.rangeTo((MAX - MIN) / STEPS).map {
            val value = MIN + it * STEPS
            value to (value == MIN || value % INTERVAL == 0)
        }

        private fun Context.createString(ms: Long): String {
            val minutes = ms / 1000 / 60
            val hrs = minutes / 60
            val min = minutes % 60
            val str = StringBuilder()
            if (hrs > 0) str.append(
                runCatching { resources.getQuantityString(R.plurals.number_hour, hrs.toInt(), hrs) }
                    .getOrNull() ?: getString(R.string.n_hours, hrs)
            ).append(if (min > 0) " " else "")
            if (min > 0) str.append(
                runCatching { resources.getQuantityString(R.plurals.number_min, min.toInt(), min) }
                    .getOrNull() ?: getString(R.string.n_minutes, min)
            )
            return str.toString()
        }
    }
}