package dev.brahmkshatriya.echo.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.extensions.Updater.ExtensionAssetResponse
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.PACKAGE_FLAGS
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.getPluginFileDir
import dev.brahmkshatriya.echo.extensions.plugger.impl.AppInfo
import dev.brahmkshatriya.echo.extensions.plugger.impl.app.ApkManifestParser
import dev.brahmkshatriya.echo.ui.extensions.ExtensionOpenerActivity
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.add.ExtensionInstallerBottomSheet
import dev.brahmkshatriya.echo.utils.AppUpdater.downloadUpdate
import dev.brahmkshatriya.echo.utils.PermsUtils.registerActivityResultLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object InstallationUtils {
    fun Context.getTempApkDir() = File(cacheDir, "apks").apply { mkdirs() }
    fun Context.cleanupTempApks() {
        getTempApkDir().deleteRecursively()
    }

    suspend fun installExtension(
        context: FragmentActivity, file: File, apk: Boolean
    ): Result<Boolean> = runCatching {
        if (apk) {
            val contentUri = FileProvider.getUriForFile(
                context, context.packageName + ".provider", file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                data = contentUri
            }
            val it = context.waitForResult(installIntent)
            if (it.resultCode == Activity.RESULT_OK) return@runCatching true
            val result = it.data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
                ?: return@runCatching false
            throw Exception(installStatusToString(result))
        } else {
            val viewModel by context.viewModel<ExtensionsViewModel>()
            val extensionLoader = viewModel.extensionLoader
            val packageInfo =
                context.packageManager.getPackageArchiveInfo(file.path, PACKAGE_FLAGS)!!
            val flow = extensionLoader.fileIgnoreFlow
            val metadata = ApkManifestParser(ImportType.File)
                .parseManifest(AppInfo(file.path, packageInfo))
            val dir = context.getPluginFileDir()
            val newFile = File(dir, "${metadata.id}.apk")
            flow.emit(newFile)
            dir.setWritable(true)
            newFile.setWritable(true)
            if (newFile.exists()) newFile.delete()
            file.copyTo(newFile, true)
            dir.setReadOnly()
            flow.emit(null)
            true
        }
    }

    suspend fun uninstallExtension(
        context: FragmentActivity, extension: Extension<*>
    ): Result<Boolean> = runCatching {
        when (extension.metadata.importType) {
            ImportType.BuiltIn -> throw UnsupportedOperationException()
            ImportType.File -> {
                val file = File(extension.metadata.path)
                val viewModel by context.viewModel<ExtensionsViewModel>()
                val extensionLoader = viewModel.extensionLoader
                val flow = extensionLoader.fileIgnoreFlow
                flow.emit(file)
                file.parentFile!!.setWritable(true)
                file.setWritable(true)
                val delete = file.delete()
                flow.emit(null)
                delete
            }

            ImportType.App -> {
                val packageName = context.getPackageName(extension.metadata.path)
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = "package:$packageName".toUri()
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                context.waitForResult(intent).resultCode == Activity.RESULT_OK
            }
        }
    }

    private fun Context.getPackageName(path: String) =
        packageManager.getPackageArchiveInfo(path, PACKAGE_FLAGS)?.packageName

    private suspend fun FragmentActivity.waitForResult(intent: Intent) = suspendCoroutine { cont ->
        val contract = ActivityResultContracts.StartActivityForResult()
        val activityResultLauncher = registerActivityResultLauncher(contract) {
            cont.resume(it)
        }
        activityResultLauncher.launch(intent)
    }

    private fun installStatusToString(status: Int?) = when (status) {
        -1 -> "INSTALL_FAILED_ALREADY_EXISTS"
        -2 -> "INSTALL_FAILED_INVALID_APK"
        -3 -> "INSTALL_FAILED_INVALID_URI"
        -4 -> "INSTALL_FAILED_INSUFFICIENT_STORAGE"
        -5 -> "INSTALL_FAILED_DUPLICATE_PACKAGE"
        -6 -> "INSTALL_FAILED_NO_SHARED_USER"
        -7 -> "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
        -8 -> "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE"
        else -> "INSTALL_FAILED : $status"
    }

    suspend fun addFromFile(context: FragmentActivity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val result = context.waitForResult(intent)
        val file = result.data?.data ?: return
        val newIntent = Intent(context, ExtensionOpenerActivity::class.java).apply {
            setData(file)
        }
        context.startActivity(newIntent)
    }

    suspend fun addExtensions(
        viewModel: ExtensionsViewModel,
        extensions: List<ExtensionAssetResponse>
    ) {
        val updater = viewModel.extensionLoader.updater
        val app = viewModel.app
        extensions.forEach { extension ->
            val url = updater.getUpdateFileUrl("", extension.updateUrl, updater.client)
                .getOrElse {
                    app.throwFlow.emit(it)
                    null
                } ?: return@forEach
            app.messageFlow.emit(
                Message(
                    app.context.getString(R.string.downloading_update_for_x, extension.name)
                )
            )
            val file = updater.runIOCatching { downloadUpdate(app.context, url, updater.client) }
                .getOrElse {
                    app.throwFlow.emit(it)
                    return@forEach
                }
            viewModel.awaitInstall(file.toUri().toString())
        }
    }

    const val EXTENSION_INSTALLER = "extensionInstaller"
    suspend fun FragmentActivity.installExtension(fileString: String) =
        suspendCancellableCoroutine {
            it.invokeOnCancellation {
                supportFragmentManager.clearFragmentResultListener(EXTENSION_INSTALLER)
            }
            supportFragmentManager.setFragmentResultListener(
                EXTENSION_INSTALLER,
                this
            ) { _, b ->
                val install = b.getBoolean("install")
                if (!install) return@setFragmentResultListener
                val file = b.getString("file")?.toUri()?.toFile()
                val installAsApk = b.getBoolean("installAsApk")
                val links = b.getStringArrayList("links").orEmpty()
                val context = this
                if (file != null && file.exists()) {
                    val extensionViewModel by viewModel<ExtensionsViewModel>()
                    lifecycleScope.launch {
                        val result = extensionViewModel.install(context, file, installAsApk)
                        if (result && installAsApk) {
                            context.createLinksDialog(file, links)
                        }
                        supportFragmentManager.clearFragmentResultListener(EXTENSION_INSTALLER)
                        runCatching { it.resume(result) }
                    }
                } else {
                    it.resume(false)
                }
            }
            ExtensionInstallerBottomSheet.newInstance(fileString).show(supportFragmentManager, null)
        }

    private suspend fun Context.createLinksDialog(
        file: File, links: List<String>
    ) = suspendCoroutine { cont ->
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.allow_opening_links))
            .setMessage(
                links.joinToString("\n") + "\n" +
                        getString(R.string.open_links_instruction)
            )
            .setPositiveButton(getString(R.string.okay)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val packageName = getPackageName(file.path)
                intent.setData("package:$packageName".toUri())
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                cont.resume(Unit)
            }
            .show()
    }
}