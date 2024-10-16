package dev.brahmkshatriya.echo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.ui.extension.ExtensionInstallerBottomSheet
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
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
                if (install && file != null) {
                    val extensionViewModel by viewModels<ExtensionViewModel>()
                    lifecycleScope.launch {
                        extensionViewModel.install(context, file, installAsApk)
                    }
                }
            }
        }
    }
}