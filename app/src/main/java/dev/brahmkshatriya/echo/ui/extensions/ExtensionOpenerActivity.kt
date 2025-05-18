package dev.brahmkshatriya.echo.ui.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toFile
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.extensions.InstallationUtils.getTempApkDir
import java.io.File

class ExtensionOpenerActivity : Activity() {
    override fun onStart() {
        super.onStart()
        val uri = intent.data

        val file = when (uri?.scheme) {
            "content" -> getTempFile(uri)
            "file" -> getTempFile(uri.toFile())
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

    private fun getTempFile(bytes: ByteArray): File {
        val tempFile = File.createTempFile("temp", ".apk", getTempApkDir())
        tempFile.writeBytes(bytes)
        return tempFile
    }

    private fun getTempFile(uri: Uri): File? {
        val stream = runCatching { contentResolver.openInputStream(uri) }.getOrNull() ?: return null
        val bytes = stream.readBytes()
        return getTempFile(bytes)
    }

    private fun getTempFile(file: File) = runCatching {
        val bytes = file.readBytes()
        getTempFile(bytes)
    }.getOrNull()
}
