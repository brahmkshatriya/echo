package dev.brahmkshatriya.echo.ui.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.databinding.DialogExtensionInstallerBinding
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionLoaderException
import dev.brahmkshatriya.echo.extensions.repo.ExtensionParser.Companion.PACKAGE_FLAGS
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoPreference.Companion.getType
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.io.File

class ExtensionInstallerBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(
            file: File,
        ) = ExtensionInstallerBottomSheet().apply {
            arguments = Bundle().apply {
                putString("file", file.absolutePath)
            }
        }

        fun Context.createLinksDialog(
            file: File, links: List<String>
        ): AlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.allow_opening_links))
            .setMessage(
                links.joinToString("\n") + "\n" + getString(R.string.open_links_instruction)
            )
            .setPositiveButton(getString(R.string.okay)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val packageName = packageManager
                    .getPackageArchiveInfo(file.absolutePath, PACKAGE_FLAGS)?.packageName
                intent.setData("package:$packageName".toUri())
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private var binding by autoCleared<DialogExtensionInstallerBinding>()
    private val viewModel by activityViewModel<ExtensionsViewModel>()

    private val args by lazy { requireArguments() }
    private val file by lazy { File(args.getString("file")!!) }
    private val supportedLinks by lazy {
        runCatching { ApkLinkParser.getSupportedLinks(file) }.getOrNull().orEmpty()
    }
    private val metadata by lazy {
        runCatching { viewModel.extensionLoader.parser.parseManifest(file, ImportType.File) }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionInstallerBinding.inflate(inflater, parent, false)
        return binding.root
    }

    private var install = false
    private var installAsApk = false

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        val metadata = metadata.getOrElse {
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
        val id = metadata.getOrNull()?.id
        val type = if (installAsApk) ImportType.App else ImportType.File
        viewModel.promptDismissed(file, install, type, id ?: "", supportedLinks)
    }
}