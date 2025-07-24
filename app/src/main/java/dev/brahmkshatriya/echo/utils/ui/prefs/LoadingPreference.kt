package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.preference.Preference
import dev.brahmkshatriya.echo.R

class LoadingPreference(context: Context) : Preference(context) {
    init {
        layoutResource = R.layout.item_loading
    }
}