package dev.brahmkshatriya.echo.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dev.brahmkshatriya.echo.R


fun AppCompatActivity.checkAudioPermissions() {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE
    checkPermissions(
        this,
        perm,
        R.string.permission_required,
        R.string.music_permission_required_summary,
        { finish() }
    )?.launch(perm)
}

fun ActivityResultCaller.checkPermissions(
    context: Context,
    perm: String,
    title: Int,
    message: Int,
    onCancel: () -> Unit,
    onGranted: () -> Unit = {},
    onRequest: () -> Unit = {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(this)
        }
    }
): ActivityResultLauncher<String>? = with(context) {
    val permStatus = ContextCompat.checkSelfPermission(this, perm)
    val contract = ActivityResultContracts.RequestPermission()
    return if (permStatus != PackageManager.PERMISSION_GRANTED) registerForActivityResult(contract) {
        if (!it) AlertDialog.Builder(this)
            .setTitle(getString(title))
            .setMessage(getString(message))
            .setPositiveButton(getString(R.string.ok)) { _, _ -> onRequest() }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                Toast.makeText(
                    this, getString(R.string.permission_denied), Toast.LENGTH_SHORT
                ).show()
                onCancel()
            }
            .show()
        else onGranted()
    } else null
}