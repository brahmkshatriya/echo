package dev.brahmkshatriya.echo.ui.extension

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.ExtensionOpenerActivity.Companion.EXTENSION_INSTALLER
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.DialogExtensionInstallerBinding
import dev.brahmkshatriya.echo.extensions.ExtensionLoadingException
import dev.brahmkshatriya.echo.extensions.getType
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.extensions.plugger.AppInfo
import dev.brahmkshatriya.echo.utils.ApkLinkParser
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.image.loadAsCircle
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import kotlinx.coroutines.launch

class ExtensionInstallerBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(
            file: String,
        ) = ExtensionInstallerBottomSheet().apply {
            arguments = Bundle().apply {
                putString("file", file)
            }
        }
    }

    private var binding by autoCleared<DialogExtensionInstallerBinding>()
    private val args by lazy { requireArguments() }
    private val file by lazy { args.getString("file")!!.toUri().toFile() }
    private val supportedLinks by lazy { ApkLinkParser.getSupportedLinks(file) }
    private val pair by lazy {
        runCatching {
            val packageInfo = requireActivity().packageManager
                .getPackageArchiveInfo(file.path, ApkPluginSource.PACKAGE_FLAGS)!!
            val type = getType(packageInfo)
            val metadata = ApkManifestParser(ImportType.App).parseManifest(
                AppInfo(file.path, packageInfo.applicationInfo!!)
            )
            type to metadata
        }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionInstallerBinding.inflate(inflater, parent, false)
        return binding.root
    }

    private var install = false
    private var installAsApk = true

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        val value = pair.getOrElse {
            val viewModel by activityViewModels<ExtensionViewModel>()
            lifecycleScope.launch {
                viewModel.throwableFlow.emit(ExtensionLoadingException(ExtensionType.MUSIC, it))
            }
            dismiss()
            return
        }
//        if (value == null) {
//            createSnack(R.string.invalid_extension)
//            dismiss()
//            return
//        }
        val (extensionType, metadata) = value

        binding.extensionTitle.text = metadata.name
        metadata.iconUrl?.toImageHolder().loadAsCircle(binding.extensionIcon, R.drawable.ic_extension) {
            binding.extensionIcon.setImageDrawable(it)
        }
        binding.extensionDetails.text = metadata.version

        val byAuthor = getString(R.string.by_author, metadata.author)
        val type = when (extensionType) {
            ExtensionType.MUSIC -> R.string.music
            ExtensionType.TRACKER -> R.string.tracker
            ExtensionType.LYRICS -> R.string.lyrics
        }
        val typeString = getString(R.string.name_extension, getString(type))
        binding.extensionDescription.text = "$typeString\n\n${metadata.description}\n\n$byAuthor"

        val isSupported = supportedLinks.isNotEmpty()
        binding.installationTypeTitle.isVisible = isSupported
        binding.installationTypeGroup.isVisible = isSupported
        binding.installationTypeSummary.isVisible = isSupported
        binding.installationTypeLinks.isVisible = isSupported
        binding.installationTypeWarning.isVisible = false

        installAsApk = isSupported
        if (isSupported) {
            binding.installationTypeLinks.text = supportedLinks.joinToString("\n")
            binding.installationTypeGroup.addOnButtonCheckedListener { group, _, _ ->
                installAsApk = group.checkedButtonId == R.id.appInstall
                binding.installationTypeWarning.isVisible = !installAsApk
            }
        }

        binding.installButton.setOnClickListener {
            install = true
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().supportFragmentManager.setFragmentResult(
            EXTENSION_INSTALLER,
            Bundle().apply {
                putString("file", file.toUri().toString())
                putBoolean("install", install)
                putBoolean("installAsApk", installAsApk)
            }
        )
    }
}