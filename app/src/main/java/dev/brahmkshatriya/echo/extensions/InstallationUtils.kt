package dev.brahmkshatriya.echo.extensions

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.extensions.ExtensionRepo.Companion.FEATURE
import dev.brahmkshatriya.echo.extensions.ExtensionRepo.Companion.getPluginFileDir
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.utils.registerActivityResultLauncher
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun installExtension(context: FragmentActivity, file: File, apk: Boolean) = runCatching {
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
        context.waitForResult(installIntent)
    } else {
        val viewModel by context.viewModels<ExtensionViewModel>()
        val extensionLoader = viewModel.extensionLoader
        val fileChangeListener = extensionLoader.fileListener
        val packageInfo = context.packageManager.getPackageArchiveInfo(
            file.path, ApkPluginSource.PACKAGE_FLAGS
        )
        val type = getType(packageInfo!!)
        val metadata = ApkManifestParser(ImportType.File)
            .parseManifest(packageInfo.applicationInfo!!)
        val flow = fileChangeListener.getFlow(type)
        val dir = context.getPluginFileDir(type)
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

suspend fun uninstallExtension(context: FragmentActivity, extension: Extension<*>) = runCatching {
    when (extension.metadata.importType) {
        ImportType.BuiltIn -> throw UnsupportedOperationException()
        ImportType.File -> {
            val file = File(extension.metadata.path)
            val viewModel by context.viewModels<ExtensionViewModel>()
            val extensionLoader = viewModel.extensionLoader
            val flow = extensionLoader.fileListener.getFlow(extension.type)
            flow.emit(file)
            file.parentFile!!.setWritable(true)
            file.setWritable(true)
            val delete = file.delete()
            flow.emit(null)
            delete
        }

        ImportType.App -> {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                extension.metadata.path, ApkPluginSource.PACKAGE_FLAGS
            )!!
            val packageName = packageInfo.packageName
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = "package:$packageName".toUri()
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            context.waitForResult(intent)
        }
    }
}

private suspend fun FragmentActivity.waitForResult(intent: Intent) = suspendCoroutine { cont ->
    val contract = ActivityResultContracts.StartActivityForResult()
    val activityResultLauncher = registerActivityResultLauncher(contract) {
        cont.resume(it.resultCode == RESULT_OK)
    }
    activityResultLauncher.launch(intent)
}


fun getType(appInfo: PackageInfo) = appInfo.reqFeatures?.find { featureInfo ->
    ExtensionType.entries.any { featureInfo.name == "$FEATURE${it.feature}" }
}?.let { featureInfo ->
    ExtensionType.entries.first { it.feature == featureInfo.name.removePrefix(FEATURE) }
} ?: error("Extension type not found for ${appInfo.packageName}")