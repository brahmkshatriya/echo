package dev.brahmkshatriya.echo.utils.ui

import com.google.android.material.checkbox.MaterialCheckBox

class CheckBoxListener(private val block: (Boolean) -> Unit) :
    MaterialCheckBox.OnCheckedStateChangedListener {
    var enabled = true
    private var checked = false
    private fun check(isChecked: Boolean) {
        if (checked == isChecked) return
        block(isChecked)
    }

    override fun onCheckedStateChangedListener(checkBox: MaterialCheckBox, state: Int) {
        val isChecked = checkBox.isChecked
        if (enabled) check(isChecked)
        checked = isChecked
    }
}