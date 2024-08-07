package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialListPreference(context: Context) : ListPreference(context) {

    private var customSummary: CharSequence? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val button = holder.findViewById(R.id.summaryButton)
        button?.setOnClickListener {
            showSummaryDialog()
        }
    }


    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        customSummary = summary
        layoutResource = if (customSummary != null){
            R.layout.preference_summary_button
        } else {
            this.layoutResource
        }
        updateSummary()
    }

    override fun onClick() {
        MaterialAlertDialogBuilder(context)
            .setSingleChoiceItems(entries, entryValues.indexOf(value)) { dialog, index ->
                if (callChangeListener(entryValues[index].toString())) {
                    setValueIndex(index)
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
        summary = entry
    }

    private fun showSummaryDialog() {
        MaterialAlertDialogBuilder(context)
            .setMessage(customSummary.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}