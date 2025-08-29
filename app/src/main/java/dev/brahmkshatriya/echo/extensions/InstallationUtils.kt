package dev.brahmkshatriya.echo.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.extensions.repo.ExtensionParser.Companion.PACKAGE_FLAGS
import dev.brahmkshatriya.echo.extensions.repo.FileRepository.Companion.getExtensionsFileDir
import dev.brahmkshatriya.echo.utils.ContextUtils.getTempApkFile
import dev.brahmkshatriya.echo.utils.PermsUtils.registerActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object InstallationUtils {

    suspend fun installApp(activity: FragmentActivity, file: File) {
        val contentUri = FileProvider.getUriForFile(
            activity, "${activity.packageName}.provider", file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            data = contentUri
        }
        val it = activity.waitForResult(installIntent)
        if (it.resultCode == Activity.RESULT_OK) return
        val result = it.data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
        throw Exception("Please uninstall the existing extension first. Error Code: $result")
    }

    suspend fun installFile(
        context: Context, fileIgnoreFlow: MutableSharedFlow<File?>, id: String, tempFile: File
    ) {
        val dir = context.getExtensionsFileDir()
        val newFile = File(dir, "$id.apk")
        dir.setWritable(true)
        newFile.setWritable(true)
        if (newFile.exists())
            if (!newFile.delete())
                throw IllegalStateException("Failed to delete existing file: $newFile")
        tempFile.renameTo(newFile)
        newFile.setWritable(false)
        dir.setReadOnly()
        fileIgnoreFlow.emit(null)
    }

    suspend fun FragmentActivity.openFileSelector(): File {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val result = waitForResult(intent)
        val uri = result.data?.data ?: throw IllegalStateException("No file selected")
        return getTempFile(uri)
    }

    fun Context.getTempFile(uri: Uri): File {
        val stream = contentResolver.openInputStream(uri)!!
        val tempFile = getTempApkFile()
        tempFile.outputStream().use { outputStream ->
            stream.copyTo(outputStream)
        }
        return tempFile
    }

    suspend fun uninstallApp(activity: FragmentActivity, path: String) {
        val packageName =
            activity.packageManager.getPackageArchiveInfo(path, PACKAGE_FLAGS)?.packageName
                ?: throw IllegalStateException("Invalid APK path or package name not found")
        activity.packageManager.getPackageInfo(packageName, 0)
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = "package:$packageName".toUri()
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        val result = activity.waitForResult(intent)
        if (result.resultCode != Activity.RESULT_OK) {
            val errorCode = result.data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
            throw Exception("Failed to uninstall extension: $errorCode")
        }
    }

    suspend fun uninstallFile(
        fileIgnoreFlow: MutableSharedFlow<File?>, path: String
    ) = withContext(Dispatchers.IO) {
        val file = File(path)
        fileIgnoreFlow.emit(file)
        file.parentFile!!.setWritable(true)
        file.setWritable(true)
        if (file.exists() && !file.delete())
            throw IllegalStateException("Failed to delete file: $file")
        fileIgnoreFlow.emit(null)
    }

    private suspend fun FragmentActivity.waitForResult(
        intent: Intent
    ) = suspendCancellableCoroutine { cont ->
        val contract = ActivityResultContracts.StartActivityForResult()
        val launcher = registerActivityResultLauncher(contract) { cont.resume(it) }
        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(intent)
    }
}