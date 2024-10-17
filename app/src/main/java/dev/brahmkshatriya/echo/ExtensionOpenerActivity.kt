package dev.brahmkshatriya.echo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.extensions.getPackageName
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
                val links = b.getStringArrayList("links").orEmpty()
                val context = this
                if (install && file != null) {
                    val extensionViewModel by viewModels<ExtensionViewModel>()
                    lifecycleScope.launch {
                        val result = extensionViewModel.install(context, file, installAsApk)
                        if (result && installAsApk) {
                            context.createLinksDialog(file, links)
                        }
                    }
                }
            }
        }

        private fun Context.createLinksDialog(
            file: File, links: List<String>
        ) = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.allow_opening_links))
            .setMessage(
                links.joinToString("\n") + "\n" +
                        getString(R.string.open_links_instruction)
            )
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val packageName = getPackageName(file.path)
                intent.setData(Uri.parse("package:$packageName"))
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }
}