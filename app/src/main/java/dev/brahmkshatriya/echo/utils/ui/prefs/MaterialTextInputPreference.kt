package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialTextInputPreference(context: Context) : EditTextPreference(context) {

    private var customSummary: CharSequence? = null

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        customSummary = summary
        updateSummary()
    }


    override fun onClick() {
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(R.layout.item_edit_text)
            .setPositiveButton(R.string.okay, null)
            .setNegativeButton(R.string.cancel, null)
            .setTitle(title)
            .create()

        dialog.setOnShowListener {
            val editText = dialog.findViewById<EditText>(R.id.edit_text)
            editText?.setText(text)
            editText?.hint = customSummary

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newText = editText?.text?.toString()
                if (callChangeListener(newText)) {
                    text = newText
                    updateSummary()
                    dialog.dismiss()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateSummary() {
        val value = context.getString(R.string.value)
        val entry = text.takeIf { !it.isNullOrEmpty() } ?: context.getString(R.string.value_not_set)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}