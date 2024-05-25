package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.preference.ListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialListPreference(context: Context) : ListPreference(context) {
    override fun onClick() {
        MaterialAlertDialogBuilder(context)
            .setSingleChoiceItems(entries, entryValues.indexOf(value)) { dialog, index ->
                if (callChangeListener(entryValues[index].toString())) setValueIndex(index)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.setTitle(title)
            .create()
            .show()
    }
}