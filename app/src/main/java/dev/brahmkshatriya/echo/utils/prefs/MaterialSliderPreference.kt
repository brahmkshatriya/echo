package dev.brahmkshatriya.echo.utils.prefs

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R

class MaterialSliderPreference(
    context: Context,
    private val from: Int,
    private val to: Int,
    private val steps: Int? = null
) :
    Preference(context) {
    init {
        layoutResource = R.layout.preference_slider
    }

    private var customSummary: CharSequence? = null
    private var defaultValue: Int? = null
    override fun onSetInitialValue(defaultValue: Any?) {
        customSummary = summary
        this.defaultValue = defaultValue as? Int
        updateSummary()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val slider = holder.itemView.findViewById<Slider>(R.id.preferences_slider)
        slider.valueFrom = from.toFloat()
        slider.valueTo = to.toFloat()
        steps?.let { slider.stepSize = it.toFloat() }
        slider.value = getPersistedInt(defaultValue ?: from).toFloat()

        slider.addOnChangeListener { _, value, _ ->
            persistInt(value.toInt())
            updateSummary()
        }
    }

    private fun updateSummary() {
        val value = context.getString(R.string.value)
        val entry = getPersistedInt(defaultValue ?: 0)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}