package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.preference.MultiSelectListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialMultipleChoicePreference(context: Context) : MultiSelectListPreference(context) {

    private var customSummary: CharSequence? = null

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        customSummary = summary
        updateSummary()
    }

    override fun onClick() {
        val selectedIndices = BooleanArray(entries.size) { index ->
            values.contains(entryValues.getOrNull(index))
        }

        MaterialAlertDialogBuilder(context)
            .setMultiChoiceItems(entries, selectedIndices) { _, which, isChecked ->
                val value = entryValues.getOrNull(which)
                if (isChecked && value != null) {
                    values.add(value.toString())
                } else {
                    values.remove(value)
                }
            }
            .setPositiveButton(R.string.okay) { dialog, _ ->
                if (callChangeListener(values)) {
                    setValues(values.toSet())
                    updateSummary()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setTitle(title)
            .create()
            .show()
    }

    private fun updateSummary() {
        val value = context.getString(R.string.value)
        val entry = values.takeIf { it.isNotEmpty() }?.joinToString(", ") { v ->
            val index = entryValues.indexOf(v)
            if (index >= 0) entries.getOrNull(index).toString() else v
        } ?: context.getString(R.string.value_not_set)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}