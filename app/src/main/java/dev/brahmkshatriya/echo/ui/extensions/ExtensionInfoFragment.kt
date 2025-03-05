package dev.brahmkshatriya.echo.ui.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.databinding.FragmentExtensionBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserBottomSheet.Companion.bind
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.settings.ExtensionFragment
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.SimpleItemSpan
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionInfoFragment : Fragment() {
    companion object {
        private fun getBundle(
            clientId: String, clientName: String, extensionType: ExtensionType
        ) = Bundle().apply {
            putString("clientId", clientId)
            putString("clientName", clientName)
            putString("extensionType", extensionType.name)
        }

        fun getBundle(extension: Extension<*>) =
            getBundle(extension.id, extension.name, extension.type)

        fun getType(type: ExtensionType) = when (type) {
            ExtensionType.MUSIC -> R.string.music
            ExtensionType.TRACKER -> R.string.tracker
            ExtensionType.LYRICS -> R.string.lyrics
            ExtensionType.MISC -> R.string.misc
        }
    }

    private var binding by autoCleared<FragmentExtensionBinding>()
    private val viewModel by activityViewModel<ExtensionsViewModel>()
    private val loginViewModel by activityViewModel<LoginUserListViewModel>()

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

        val extension =
            viewModel.extensions.getFlow(extensionType).value?.find { it.id == clientId }
        if (extension == null) {
            parentFragmentManager.popBackStack()
            return
        }

        val metadata = extension.metadata
        if (metadata.importType != ImportType.BuiltIn) {
            binding.toolBar.inflateMenu(R.menu.extensions_menu)
            if (metadata.repoUrl == null) {
                binding.toolBar.menu.removeItem(R.id.menu_repo)
            }
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

                    R.id.menu_repo -> {
                        requireActivity().openLink(metadata.repoUrl!!)
                        true
                    }

                    else -> false
                }
            }
        }

        metadata.icon
            .loadAsCircle(binding.extensionIcon, R.drawable.ic_extension_48dp) {
                binding.extensionIcon.setImageDrawable(it)
            }
        binding.extensionDetails.text =
            "${metadata.version} • ${metadata.importType.name}"

        val byAuthor = getString(R.string.by_x, metadata.author)
        val type = getType(extensionType)
        val typeString = getString(R.string.x_extension, getString(type))
        val span = SpannableString("$typeString\n\n${metadata.description}\n\n$byAuthor")
        val authUrl = metadata.authorUrl
        if (authUrl != null) {
            val itemSpan = SimpleItemSpan(requireContext()) {
                requireActivity().openLink(authUrl)
            }
            val start = span.length - metadata.author.length
            span.setSpan(itemSpan, start, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.extensionDescription.text = span
        binding.extensionDescription.movementMethod = LinkMovementMethod.getInstance()

        fun updateText(enabled: Boolean) {
            binding.enabledText.text = getString(
                if (enabled) R.string.enabled else R.string.disabled
            )
        }
        binding.enabledSwitch.apply {
            updateText(metadata.isEnabled)
            isChecked = metadata.isEnabled
            setOnCheckedChangeListener { _, isChecked ->
                updateText(isChecked)
                viewModel.setExtensionEnabled(extensionType, metadata.id, isChecked)
            }
            binding.enabledCont.setOnClickListener { toggle() }
        }

        lifecycleScope.launch {
            runCatching {
                if (extension.isClient<LoginClient>()) {
                    loginViewModel.currentExtension.value = extension
                    binding.extensionLoginUser.bind(this@ExtensionInfoFragment) {}
                } else binding.extensionLoginUser.root.isVisible = false
            }
        }
        binding.extensionSettings.transitionName = "setting_${metadata.id}"
        binding.extensionSettings.setOnClickListener {
            openFragment<ExtensionFragment>(it, ExtensionFragment.getBundle(extension))
        }
    }

    private fun Activity.openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
        startActivity(intent)
    }

    override fun onDestroy() {
        loginViewModel.currentExtension.value = viewModel.extensions.current.value
        super.onDestroy()
    }
}