package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class TransitionPreference(
    context: Context
) : Preference(context) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.id = key.hashCode()
        holder.itemView.transitionName = key
    }
}