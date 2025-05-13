package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

open class LongClickPreference(context:Context) : Preference(context) {
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