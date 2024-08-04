package dev.brahmkshatriya.echo.ui.player

import com.google.android.material.checkbox.MaterialCheckBox

class CheckBoxListener(private val block: (Boolean) -> Unit) :
    MaterialCheckBox.OnCheckedStateChangedListener {
    var enabled = true
    var checked = false
    private fun check(isChecked: Boolean) {
        if(checked == isChecked) return
        checked = isChecked
        block(isChecked)
    }

    override fun onCheckedStateChangedListener(checkBox: MaterialCheckBox, state: Int) {
        if (enabled) when (state) {
            MaterialCheckBox.STATE_CHECKED -> check(true)
            else -> check(false)
        }
    }
}