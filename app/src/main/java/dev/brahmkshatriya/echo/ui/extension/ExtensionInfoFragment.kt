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
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentExtensionBinding
import dev.brahmkshatriya.echo.plugger.echo.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.echo.getExtension
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

        fun newInstance(metadata: ExtensionMetadata, extensionType: Int) =
            newInstance(metadata.id, metadata.name, ExtensionType.entries[extensionType])
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

        val pair = when (extensionType) {
            ExtensionType.MUSIC -> {
                val extension = viewModel.extensionListFlow.getExtension(clientId)
                if (extension == null) null else extension.metadata to extension.client
            }

            ExtensionType.TRACKER -> {
                val extension = viewModel.trackerListFlow.getExtension(clientId)
                if (extension == null) null else extension.metadata to extension.client
            }

            ExtensionType.LYRICS -> {
                val extension = viewModel.lyricsListFlow.getExtension(clientId)
                if (extension == null) null else extension.metadata to extension.client
            }
        }

        if (pair == null) {
            createSnack(requireContext().noClient())
            parentFragmentManager.popBackStack()
            return
        }

        val (metadata, client) = pair

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

        if (client is LoginClient) {
            val loginViewModel by activityViewModels<LoginUserViewModel>()
            loginViewModel.currentExtension.value = LoginUserViewModel.ExtensionData(
                extensionType, metadata, client
            )
            binding.extensionLoginUser.bind(this) {}
        } else binding.extensionLoginUser.root.isVisible = false

        binding.extensionSettings.transitionName = "setting_${metadata.id}"
        binding.extensionSettings.setOnClickListener {
            openFragment(ExtensionFragment.newInstance(metadata, extensionType), it)
        }
    }

}