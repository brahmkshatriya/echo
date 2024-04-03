package dev.brahmkshatriya.echo.ui.player

import com.google.android.material.checkbox.MaterialCheckBox

class CheckBoxListener(private val block: (Boolean) -> Unit) :
    MaterialCheckBox.OnCheckedStateChangedListener {
    var enabled = true
    override fun onCheckedStateChangedListener(checkBox: MaterialCheckBox, state: Int) {
        if (enabled) when (state) {
            MaterialCheckBox.STATE_CHECKED -> block(true)
            else -> block(false)
        }
    }
}