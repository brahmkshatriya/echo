package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

open class ClickPreference(context:Context) : Preference(context) {
    private var onClickListener: (() -> Unit)? = null

    fun setOnClickListener(listener: () -> Unit) {
        onClickListener = listener
    }
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val listener = onClickListener ?: return
        holder.itemView.setOnClickListener {
            listener.invoke()
        }
    }
}