package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentMainBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.main.home.HomeFragment
import dev.brahmkshatriya.echo.ui.main.library.LibraryFragment
import dev.brahmkshatriya.echo.ui.main.search.SearchFragment
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MainFragment : Fragment() {

    private var binding by autoCleared<FragmentMainBinding>()
    private val viewModel by activityViewModel<UiViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        val adapter = MainAdapter(this)
        binding.root.adapter = adapter
        binding.root.isUserInputEnabled = false
        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.navigation.value = 0
            }
        }
        observe(viewModel.navigation) {
            backCallback.isEnabled = it != 0
            binding.root.setCurrentItem(it, false)
        }
        requireActivity().onBackPressedDispatcher.addCallback(backCallback)
    }

    class MainAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> SearchFragment()
                2 -> LibraryFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }

    companion object {
        fun RecyclerView.first() =
            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        fun RecyclerView.scrollTo(position: Int, block: (Int) -> Unit) = doOnLayout {
            if (position < 1) return@doOnLayout
            (layoutManager as LinearLayoutManager).run {
                scrollToPositionWithOffset(position, 0)
                post { runCatching { block(findFirstVisibleItemPosition()) } }
            }
        }

        fun MaterialToolbar.configureMainMenu(fragment: MainFragment) {
            val extVM by fragment.activityViewModel<ExtensionsViewModel>()
            val loginUserViewModel by fragment.activityViewModel<LoginUserListViewModel>()
            val settingsMenu = findViewById<View>(R.id.menu_settings)
            settingsMenu.transitionName = "settings"
            val extMenu = findViewById<View>(R.id.menu_extensions)
            extMenu.transitionName = "extensions"

            fragment.observe(extVM.extensions.current) { extension ->
                extension?.metadata?.iconUrl?.toImageHolder()
                    .loadAsCircle(extMenu, R.drawable.ic_extension) {
                        menu.findItem(R.id.menu_extensions).icon = it
                    }
                loginUserViewModel.currentExtension.value = extension
            }
            extMenu.setOnClickListener {
                ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                    .show(fragment.parentFragmentManager, null)
            }
            extMenu.setOnLongClickListener {
                extVM.extensions.run {
                    val list = music.value?.filter { it.isEnabled } ?: return@run false
                    val index = list.indexOf(current.value)
                    if (index == -1) return@run false
                    val next = list[(index + 1) % list.size]
                    if (next == current.value) return@run false
                    setupMusicExtension(next)
                    true
                }
            }

            fragment.observe(loginUserViewModel.allUsers) { (extension, all) ->
                val isLoginClient = extension?.isClient<LoginClient>() ?: false
                val user = loginUserViewModel.currentUser.value.second
                if (isLoginClient) {
                    user?.cover.loadAsCircle(settingsMenu, R.drawable.ic_account_circle_48dp) {
                        menu.findItem(R.id.menu_settings).icon = it
                    }
                    settingsMenu.setOnClickListener {
                        LoginUserBottomSheet().show(fragment.parentFragmentManager, null)
                    }
                    settingsMenu.setOnLongClickListener {
                        val ext = extension ?: return@setOnLongClickListener false
                        val index = all?.indexOf(user) ?: return@setOnLongClickListener false
                        if (index == -1) return@setOnLongClickListener false
                        val next = all[(index + 1) % all.size]
                        if (next == user) return@setOnLongClickListener false
                        loginUserViewModel.setLoginUser(next.toEntity(ext.type, ext.id))
                        true
                    }
                } else {
                    menu.findItem(R.id.menu_settings).setIcon(R.drawable.ic_settings_outline)
                    settingsMenu.setOnClickListener {
                        fragment.openFragment(SettingsFragment(), settingsMenu)
                    }
                    settingsMenu.setOnLongClickListener(null)
                }

            }
        }

    }
}