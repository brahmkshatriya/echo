package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.preference.MultiSelectListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialMultipleChoicePreference(context: Context) : MultiSelectListPreference(context) {

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
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
            .setPositiveButton(R.string.ok) { dialog, _ ->
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
        summary = values.joinToString(", ") { value ->
            val index = entryValues.indexOf(value)
            if (index >= 0) entries.getOrNull(index).toString() else value
        }
    }
}