package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toUser
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.extension.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.login.LoginUserBottomSheet
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel

fun MaterialToolbar.configureMainMenu(fragment: MainFragment) {
    val extensionViewModel by fragment.activityViewModels<ExtensionViewModel>()
    val loginUserViewModel by fragment.activityViewModels<LoginUserViewModel>()
    val settings = findViewById<View>(R.id.menu_settings)
    settings.transitionName = "settings"
    val extensions = findViewById<View>(R.id.menu_extensions)
    extensions.transitionName = "extensions"

    fragment.observe(extensionViewModel.extensionFlow) { client ->
        client?.metadata?.iconUrl?.toImageHolder().loadWith(extensions, R.drawable.ic_extension) {
            menu.findItem(R.id.menu_extensions).icon = it
        }
    }
    extensions.setOnClickListener {
        ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
            .show(fragment.parentFragmentManager, null)
    }
    extensions.setOnLongClickListener {
        extensionViewModel.run {
            val list = extensionListFlow.value?.filter { it.metadata.enabled } ?: return@run false
            val index = list.indexOf(currentExtension)
            if (index == -1) return@run false
            val next = list[(index + 1) % list.size]
            setExtension(next)
            true
        }
    }

    fragment.observe(loginUserViewModel.extensionLoader.currentWithUser) { (extension, u) ->
        val user = u?.toUser()
        val isLoginClient = extension?.isClient<LoginClient>() ?: false
        if (isLoginClient) {
            user?.cover.loadWith(settings, R.drawable.ic_account_circle_48dp) {
                menu.findItem(R.id.menu_settings).icon = it
            }
        } else menu.findItem(R.id.menu_settings).setIcon(R.drawable.ic_settings_outline)

        settings.setOnClickListener {
            if (isLoginClient) {
                loginUserViewModel.currentExtension.value = extension
                LoginUserBottomSheet()
                    .show(fragment.parentFragmentManager, null)
            } else fragment.openFragment(SettingsFragment(), settings)
        }
    }
}


