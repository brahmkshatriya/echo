package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat

class SwitchLongClickPreference(context: Context) : SwitchPreferenceCompat(context) {
    private var onLongClickListener: (() -> Unit)? = null

    fun setOnLongClickListener(listener: () -> Unit) {
        onLongClickListener = listener
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val listener = onLongClickListener ?: return
        holder.itemView.setOnLongClickListener {
            listener.invoke()
            true
        }
    }
}