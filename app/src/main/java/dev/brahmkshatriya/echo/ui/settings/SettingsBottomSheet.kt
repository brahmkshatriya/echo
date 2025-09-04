package dev.brahmkshatriya.echo.ui.settings

import android.graphics.drawable.Animatable
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.MainApplication.Companion.getCurrentLanguage
import dev.brahmkshatriya.echo.MainApplication.Companion.languages
import dev.brahmkshatriya.echo.MainApplication.Companion.setCurrentLanguage
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.databinding.DialogSettingsBinding
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadFragment
import dev.brahmkshatriya.echo.ui.download.DownloadViewModel
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoFragment.Companion.openLink
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.add.ExtensionsAddBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.extensions.manage.ManageExtensionsFragment
import dev.brahmkshatriya.echo.ui.main.HeaderAdapter.Companion.loadBigIcon
import dev.brahmkshatriya.echo.ui.main.HeaderAdapter.Companion.setLoopedLongClick
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.ContextUtils.copyToClipboard
import dev.brahmkshatriya.echo.utils.ContextUtils.getArch
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureBottomBar
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.random.Random

class SettingsBottomSheet : BottomSheetDialogFragment(R.layout.dialog_settings) {
    private val viewModel by activityViewModel<LoginUserListViewModel>()
    private val downloadVM by activityViewModel<DownloadViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DialogSettingsBinding.bind(view)

        binding.closeButton.setOnClickListener { dismiss() }
        binding.logoImage.setOnClickListener {
            binding.logoImage.setImageResource(R.drawable.art_splash_anim)
            (binding.logoImage.drawable as Animatable).start()
            when (Random.nextInt(5)) {
                0 -> Toast.makeText(it.context, "Ooo what do we have here?", Toast.LENGTH_SHORT)
                    .show()

                2 -> Toast.makeText(it.context, "Nothing to see here.", Toast.LENGTH_SHORT).show()
            }
        }
        (binding.logoImage.drawable as Animatable).start()

        binding.player.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsPlayerFragment>()
        }

        binding.lookAndFeel.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsLookFragment>()
        }

        binding.other.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsOtherFragment>()
        }

        binding.downloads.setOnClickListener {
            dismiss()
            requireActivity().openFragment<DownloadFragment>()
        }

        observe(downloadVM.downloadExtension) { extension ->
            binding.downloadSettings.isEnabled = extension != null
            binding.downloadSettings.setOnClickListener {
                dismiss()
                extension ?: return@setOnClickListener
                requireActivity().openFragment<ExtensionInfoFragment>(
                    null, ExtensionInfoFragment.getBundle(extension)
                )
            }
        }

        binding.manageExtension.setOnClickListener {
            dismiss()
            requireActivity().openFragment<ManageExtensionsFragment>()
        }

        binding.addExtension.setOnClickListener {
            dismiss()
            ExtensionsAddBottomSheet().show(parentFragmentManager, null)
        }

        val settings = requireContext().getSettings()
        val language = getCurrentLanguage(settings)
        val languages = mapOf("system" to getString(R.string.system)) + languages
        val langList = languages.entries.toList()
        binding.language.run {
            text = getString(R.string.language_x, languages[language])
            setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setSingleChoiceItems(
                        langList.map { it.value }.toTypedArray(),
                        langList.indexOfFirst { it.key == language }
                    ) { dialog, which ->
                        setCurrentLanguage(settings, langList[which].key)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setTitle(getString(R.string.select_language))
                    .create()
                    .show()
                dismiss()
            }
        }

        binding.wiki.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://wotaku.wiki/guides/music/echo")
        }

        val repo = getString(R.string.app_github_repo)
        binding.contributors.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://github.com/$repo/graphs/contributors")
        }

        binding.donate.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://ko-fi.com/brahmkshatriya")
        }

        binding.discord.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://discord.gg/J3WvbBUU8Z")
        }

        binding.github.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://github.com/$repo")
        }

        binding.telegram.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://t.me/echo_extension")
        }

        binding.version.run {
            val version = appVersion()
            text = version
            setOnClickListener {
                val info = buildString {
                    appendLine("Echo Version: $version")
                    appendLine("Device: $BRAND $DEVICE")
                    appendLine("Architecture: ${getArch()}")
                    appendLine("OS Version: $CODENAME $RELEASE ($SDK_INT)")
                }
                context.copyToClipboard(getString(R.string.version), info)
            }
            setOnLongClickListener {
                val viewModel by activityViewModel<ExtensionsViewModel>()
                viewModel.update(requireActivity(), true)
                dismiss()
                true
            }
        }

        binding.shivam.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://github.com/brahmkshatriya")
        }

        configureBottomBar(binding.extensionBar)
        binding.extensionsCont.setOnClickListener {
            dismiss()
            ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                .show(parentFragmentManager, null)
        }

        observe(viewModel.extensionLoader.current) { ext ->
            viewModel.currentExtension.value = ext
            binding.currentExtensionSettings.setOnClickListener {
                ext ?: return@setOnClickListener
                dismiss()
                requireActivity().openFragment<ExtensionInfoFragment>(
                    null, ExtensionInfoFragment.getBundle(ext)
                )
            }
            binding.extensions.loadBigIcon(ext?.metadata?.icon, R.drawable.ic_extension_32dp)
            binding.extensionsCont.setLoopedLongClick(
                viewModel.extensionLoader.music.value.filter { it.isEnabled },
                { viewModel.extensionLoader.current.value }
            ) {
                viewModel.extensionLoader.setupMusicExtension(it, true)
            }
        }

        binding.currentAccountName.setOnClickListener {
            dismiss()
            LoginUserListBottomSheet().show(parentFragmentManager, null)
        }
        binding.accountsCont.setOnClickListener {
            dismiss()
            LoginUserListBottomSheet().show(parentFragmentManager, null)
        }
        observe(viewModel.allUsersWithClient) { (ext, isLoginClient, all) ->
            binding.extensionBar.updateLayoutParams {
                width = if (isLoginClient) MATCH_PARENT else WRAP_CONTENT
            }
            binding.currentAccountName.run {
                isVisible = isLoginClient
                text = viewModel.currentUser.value?.name ?: getString(R.string.no_account)
                setLoopedLongClick(all, { all.find { it.second } }) {
                    ext ?: return@setLoopedLongClick
                    viewModel.setLoginUser(it.first.toEntity(ext.type, ext.id))
                }
            }
            binding.accounts.loadBigIcon(
                viewModel.currentUser.value?.cover, R.drawable.ic_account_circle_32dp
            )
            binding.accountsCont.run {
                isVisible = isLoginClient
                setLoopedLongClick(all, { all.find { it.second } }) {
                    ext ?: return@setLoopedLongClick
                    viewModel.setLoginUser(it.first.toEntity(ext.type, ext.id))
                }
            }
        }
    }
}