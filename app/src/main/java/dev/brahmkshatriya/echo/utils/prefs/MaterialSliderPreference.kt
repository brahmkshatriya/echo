package dev.brahmkshatriya.echo.utils.prefs

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R

class MaterialSliderPreference(
    context: Context,
    private val from: Int,
    private val to: Int,
    private val steps: Int? = null,
    private val allowOverride: Boolean = false
) : Preference(context) {
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
        val max = if (allowOverride) getPersistedInt(to).coerceAtLeast(to) else to
        slider.valueTo = max.toFloat()
        slider.stepSize = steps?.toFloat() ?: 1f
        slider.value = getPersistedInt(defaultValue ?: from).toFloat()

        slider.addOnChangeListener { _, value, byUser ->
            persistInt(value.toInt())
            updateSummary()
            if (allowOverride && !dialogOpened && byUser && value == slider.valueTo) {
                showOverrideDialog(slider, value)
            }
        }

        if(allowOverride) holder.itemView.setOnClickListener {
            showOverrideDialog(slider, slider.value)
        }
    }

    private var dialogOpened = false
    private fun showOverrideDialog(slider: Slider, value: Float) {
        dialogOpened = true
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(R.layout.dialog_edit_text)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setTitle(title)
            .create()

        dialog.setOnShowListener {
            val editText = dialog.findViewById<EditText>(R.id.edit_text)
            editText?.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            editText?.setText(value.toInt().toString())
            editText?.hint = customSummary

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newMaxValue = editText?.text?.toString()?.toIntOrNull()
                if (newMaxValue != null && newMaxValue > from) {
                    slider.valueTo = newMaxValue.toFloat()
                    slider.value = newMaxValue.toFloat()
                    dialog.dismiss()
                } else {
                    editText?.error = context.getString(R.string.error)
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { dialogOpened = false }
        dialog.show()
    }

    private fun updateSummary() {
        val value = context.getString(R.string.value)
        val entry = getPersistedInt(defaultValue ?: 0)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}