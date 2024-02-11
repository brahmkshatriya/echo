package dev.brahmkshatriya.echo.ui.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun checkPermissions(activity: AppCompatActivity) {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE
    val permStatus = ContextCompat.checkSelfPermission(activity, perm)
    if (permStatus != PackageManager.PERMISSION_GRANTED)
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it)
                AlertDialog.Builder(activity)
                    .setTitle("Permission Required")
                    .setMessage("This permission is required to access your music library")
                    .setPositiveButton("Ok") { _, _ ->
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activity.packageName, null)
                            activity.startActivity(this)
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ -> activity.finish() }
                    .show()

        }.launch(perm)
}