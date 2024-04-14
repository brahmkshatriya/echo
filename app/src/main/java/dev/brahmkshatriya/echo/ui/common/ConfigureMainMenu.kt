package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.ui.extension.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.login.LoginUserBottomSheet
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

fun MaterialToolbar.configureMainMenu(fragment: MainFragment) {
    val extensionViewModel by fragment.activityViewModels<ExtensionViewModel>()
    val loginUserViewModel by fragment.activityViewModels<LoginUserViewModel>()
    val settings = findViewById<View>(R.id.menu_settings)
    settings.transitionName = "settings"
    val extensions = findViewById<View>(R.id.menu_extensions)
    extensions.transitionName = "extensions"

    fragment.observe(extensionViewModel.extensionFlow) { client ->
        client?.metadata?.iconUrl.loadWith(extensions, R.drawable.ic_extension) {
            menu.findItem(R.id.menu_extensions).icon = it
        }
    }
    extensions.setOnClickListener {
        ExtensionsListBottomSheet().show(fragment.parentFragmentManager, null)
    }
    extensions.setOnLongClickListener {
        extensionViewModel.run {
            val list = extensionListFlow.flow.value ?: return@run false
            val index = list.indexOf(currentExtension)
            val next = list[(index + 1) % list.size]
            setExtension(next)
            true
        }
    }

    fragment.observe(loginUserViewModel.currentUser) { (client, user) ->
        if (client is LoginClient) {
            user?.cover.loadWith(settings, R.drawable.ic_account_circle) {
                menu.findItem(R.id.menu_settings).icon = it
            }
        }
        else menu.findItem(R.id.menu_settings).setIcon(R.drawable.ic_settings_outline)

        settings.setOnClickListener {
            if (client is LoginClient) {
                LoginUserBottomSheet()
                    .show(fragment.parentFragmentManager, null)
            }
            else fragment.openFragment(SettingsFragment(), settings)
        }
    }
}


