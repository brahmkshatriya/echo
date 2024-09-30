package dev.brahmkshatriya.echo.utils.prefs

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
            .setView(R.layout.dialog_edit_text)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setTitle(title)
            .create()

        dialog.setOnShowListener {
            val editText = dialog.findViewById<EditText>(R.id.edit_text)
            editText?.setText(text)
            editText?.hint = summary

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newText = editText?.text?.toString()
                if (callChangeListener(newText)) {
                    text = newText
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
        val entry = text ?: context.getString(R.string.str_not_set)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}