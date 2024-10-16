package dev.brahmkshatriya.echo.ui.extension

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentExtensionBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.login.LoginUserBottomSheet.Companion.bind
import dev.brahmkshatriya.echo.ui.settings.ExtensionFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.launch

class ExtensionInfoFragment : Fragment() {
    companion object {
        fun newInstance(
            clientId: String, clientName: String, extensionType: ExtensionType
        ) = ExtensionInfoFragment().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putString("clientName", clientName)
                putString("extensionType", extensionType.name)
            }
        }

        fun newInstance(extension: Extension<*>) =
            newInstance(extension.id, extension.name, extension.type)
    }

    private var binding by autoCleared<FragmentExtensionBinding>()
    private val viewModel by activityViewModels<ExtensionViewModel>()

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val clientName by lazy { args.getString("clientName")!! }
    private val extensionType by lazy {
        ExtensionType.valueOf(args.getString("extensionType")!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExtensionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.iconContainer.updatePadding(top = it.top)
            binding.nestedScrollView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolBar.title = clientName

        val extension = when (extensionType) {
            ExtensionType.MUSIC -> viewModel.extensionListFlow.getExtension(clientId)
            ExtensionType.TRACKER -> viewModel.trackerListFlow.getExtension(clientId)
            ExtensionType.LYRICS -> viewModel.lyricsListFlow.getExtension(clientId)
        }

        if (extension == null) {
            createSnack(requireContext().noClient())
            parentFragmentManager.popBackStack()
            return
        }

        val metadata = extension.metadata

        if (metadata.importType != ImportType.BuiltIn) {
            binding.toolBar.inflateMenu(R.menu.extensions_menu)
            binding.toolBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_uninstall -> {
                        lifecycleScope.launch {
                            viewModel.uninstall(requireActivity(), extension) {
                                parentFragmentManager.popBackStack()
                            }
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        metadata.iconUrl?.toImageHolder().loadWith(binding.extensionIcon, R.drawable.ic_extension) {
            binding.extensionIcon.setImageDrawable(it)
        }
        binding.extensionDetails.text =
            "${metadata.version} • ${metadata.importType.name}"

        val byAuthor = getString(R.string.by_author, metadata.author)
        val type = when (extensionType) {
            ExtensionType.MUSIC -> R.string.music
            ExtensionType.TRACKER -> R.string.tracker
            ExtensionType.LYRICS -> R.string.lyrics
        }
        val typeString = getString(R.string.name_extension, getString(type))
        binding.extensionDescription.text = "$typeString\n\n${metadata.description}\n\n$byAuthor"

        fun updateText(enabled: Boolean) {
            binding.enabledText.text = getString(
                if (enabled) R.string.enabled else R.string.disabled
            )
        }
        binding.enabledSwitch.apply {
            updateText(metadata.enabled)
            isChecked = metadata.enabled
            setOnCheckedChangeListener { _, isChecked ->
                updateText(isChecked)
                viewModel.setExtensionEnabled(extensionType, metadata.id, isChecked)
            }
            binding.enabledCont.setOnClickListener { toggle() }
        }

        if (extension.isClient<LoginClient>()) {
            val loginViewModel by activityViewModels<LoginUserViewModel>()
            loginViewModel.currentExtension.value = extension
            binding.extensionLoginUser.bind(this) {}
        } else binding.extensionLoginUser.root.isVisible = false

        binding.extensionSettings.transitionName = "setting_${metadata.id}"
        binding.extensionSettings.setOnClickListener {
            openFragment(ExtensionFragment.newInstance(metadata, extensionType), it)
        }
    }

}