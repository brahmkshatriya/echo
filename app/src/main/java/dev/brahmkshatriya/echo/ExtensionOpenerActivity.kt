package dev.brahmkshatriya.echo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.ui.extension.ApkLinkParser
import java.io.File


class ExtensionOpenerActivity : Activity() {
    override fun onStart() {
        super.onStart()
        val uri = intent.data
        val file = when (uri?.scheme) {
            "content" -> getTempFile(uri)
            else -> null
        }
        finish()
        if (file == null)
            Toast.makeText(this, "Could not find a file.", Toast.LENGTH_SHORT).show()
        val startIntent = Intent(this, MainActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startIntent.data = file?.let { Uri.fromFile(it) }
        startActivity(startIntent)
    }

    private fun getTempFile(uri: Uri): File? {
        val stream = contentResolver.openInputStream(uri) ?: return null
        val bytes = stream.readBytes()
        val tempFile = File.createTempFile("temp", ".apk", cacheDir)
        tempFile.writeBytes(bytes)
        return tempFile
    }

    companion object {
        const val EXTENSION_INSTALLER = "extensionInstaller"
        fun FragmentActivity.openExtensionInstaller(uri: Uri) {
            val apk = uri.toFile()
            val supportedLinks = ApkLinkParser.getSupportedLinks(apk)


            supportFragmentManager.setFragmentResultListener(EXTENSION_INSTALLER, this) { _, bundle ->
                val file = bundle.getString("file")?.toUri()?.toFile()
                file?.delete()
            }
        }
    }
}