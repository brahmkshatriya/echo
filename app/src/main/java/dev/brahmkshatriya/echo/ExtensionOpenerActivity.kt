package dev.brahmkshatriya.echo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.extensions.ExtensionRepo.Companion.FEATURE
import dev.brahmkshatriya.echo.extensions.ExtensionRepo.Companion.getPluginFileDir
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.extensions.plugger.FileChangeListener
import dev.brahmkshatriya.echo.ui.extension.ExtensionInstallerBottomSheet
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import kotlinx.coroutines.launch
import java.io.File


class ExtensionOpenerActivity : Activity() {
    override fun onStart() {
        super.onStart()
        val uri = intent.data

        val file = when (uri?.scheme) {
            "content" -> getTempFile(uri)
            else -> null
        }

        if (file == null) Toast.makeText(
            this, getString(R.string.could_not_find_the_file), Toast.LENGTH_SHORT
        ).show()

        finish()
        val startIntent = Intent(this, MainActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startIntent.data = file?.let { Uri.fromFile(it) }
        startActivity(startIntent)
    }

    private fun getTempFile(uri: Uri): File? {
        val stream = contentResolver.openInputStream(uri) ?: return null
        val bytes = stream.readBytes()
        val tempFile = File.createTempFile("temp", ".apk", getTempApkDir())
        tempFile.writeBytes(bytes)
        return tempFile
    }

    companion object {
        const val EXTENSION_INSTALLER = "extensionInstaller"

        fun Context.getTempApkDir() = File(cacheDir, "apks").apply { mkdirs() }

        fun Context.cleanupTempApks() {
            getTempApkDir().deleteRecursively()
        }

        fun FragmentActivity.openExtensionInstaller(uri: Uri) {

            ExtensionInstallerBottomSheet.newInstance(uri.toString())
                .show(supportFragmentManager, null)

            supportFragmentManager.setFragmentResultListener(EXTENSION_INSTALLER, this) { _, b ->
                val file = b.getString("file")?.toUri()?.toFile()
                val install = b.getBoolean("install")
                val installAsApk = b.getBoolean("installAsApk")
                val context = this
                if (install && file != null) lifecycleScope.launch {
                    val installation = if (installAsApk) openApk(context, file)
                    else {
                        val viewModel by viewModels<LoginUserViewModel>()
                        val extensionLoader = viewModel.extensionLoader
                        installAsFile(context, file, extensionLoader.fileListener)
                    }
                    val exception = installation.exceptionOrNull()
                    if (exception != null) {
                        val viewModel by viewModels<SnackBar>()
                        viewModel.throwableFlow.emit(exception)
                    } else if (!installAsApk)
                        createSnack(getString(R.string.extension_installed_successfully))
                }
            }
        }

        private fun openApk(context: Context, file: File) = runCatching {
            val contentUri = FileProvider.getUriForFile(
                context, context.packageName + ".provider", file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                data = contentUri
            }
            context.startActivity(installIntent)
        }

        private suspend fun installAsFile(
            context: Context, file: File, fileChangeListener: FileChangeListener
        ) = runCatching {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                file.path, ApkPluginSource.PACKAGE_FLAGS
            )
            val type = getType(packageInfo!!)
            val metadata = ApkManifestParser(ImportType.File)
                .parseManifest(packageInfo.applicationInfo!!)
            val dir = context.getPluginFileDir(type)
            dir.setWritable(true)
            val newFile = File(dir, "${metadata.id}.apk")
            val flow = fileChangeListener.getFlow(type)
            flow.emit(newFile)
            newFile.setWritable(true)
            if (newFile.exists()) newFile.delete()
            file.copyTo(newFile, true)
            dir.setReadOnly()
            flow.emit(null)
        }

        fun getType(appInfo: PackageInfo) = appInfo.reqFeatures?.find { featureInfo ->
            ExtensionType.entries.any { featureInfo.name == "$FEATURE${it.feature}" }
        }?.let { featureInfo ->
            ExtensionType.entries.first { it.feature == featureInfo.name.removePrefix(FEATURE) }
        } ?: error("Extension type not found for ${appInfo.packageName}")
    }
}