package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.common.clients.ExtensionClient

class ExtensionFragment(private val extension: ExtensionClient) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceManager.sharedPreferencesName = extension.metadata.id
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        extension.setupPreferenceSettings(screen)
    }

//    data class PreferenceLayouts(
//        val preference: Int,
//        val switch: Int
//    )
}