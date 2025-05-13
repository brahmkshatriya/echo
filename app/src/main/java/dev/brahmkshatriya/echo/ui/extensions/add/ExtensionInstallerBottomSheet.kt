package dev.brahmkshatriya.echo.ui.extensions.add

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.databinding.DialogExtensionInstallerBinding
import dev.brahmkshatriya.echo.extensions.InstallationUtils.EXTENSION_INSTALLER
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionLoaderException
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.PACKAGE_FLAGS
import dev.brahmkshatriya.echo.extensions.plugger.impl.AppInfo
import dev.brahmkshatriya.echo.extensions.plugger.impl.app.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.impl.file.ApkLinkParser
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoPreference.Companion.getType
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

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
    private val metadata by lazy {
        runCatching {
            val packageInfo = requireActivity().packageManager
                .getPackageArchiveInfo(file.path, PACKAGE_FLAGS)!!
            val metadata = ApkManifestParser(ImportType.App).parseManifest(
                AppInfo(file.path, packageInfo)
            )
            metadata
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
        val metadata = metadata.getOrElse {
            val viewModel by activityViewModel<ExtensionsViewModel>()
            lifecycleScope.launch {
                viewModel.app.throwFlow.emit(
                    ExtensionLoaderException("ExtensionInstaller", file.toString(), it)
                )
            }
            dismiss()
            return
        }

        binding.extensionTitle.text = metadata.name
        metadata.icon
            .loadAsCircle(binding.extensionIcon, R.drawable.ic_extension_48dp) {
                binding.extensionIcon.setImageDrawable(it)
            }
        binding.extensionDetails.text = metadata.version

        val byAuthor = getString(R.string.by_x, metadata.author)
        val type = getType(metadata.type)
        val typeString = getString(R.string.x_extension, getString(type))
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