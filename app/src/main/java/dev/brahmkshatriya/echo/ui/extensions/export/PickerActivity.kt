package dev.brahmkshatriya.echo.ui.extensions.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PickerActivity : AppCompatActivity() {
    sealed interface PickerType {
        object Folder : PickerType
        object File : PickerType
    }

    interface Callback {
        fun onSelected(uri: Uri)
        fun onCancelled() {}
    }

    companion object {
        private var callback: Callback? = null
        private var pickerType: PickerType? = null

        fun openFolder(context: Context, callback: Callback) {
            open(context, PickerType.Folder, callback)
        }

        fun openFolder(context: Context, onSelected: (Uri) -> Unit) {
            openFolder(context, object : Callback {
                override fun onSelected(uri: Uri) = onSelected(uri)
            })
        }

        fun openFile(context: Context, callback: Callback) {
            open(context, PickerType.File, callback)
        }

        fun openFile(context: Context, onSelected: (Uri) -> Unit) {
            openFile(context, object : Callback {
                override fun onSelected(uri: Uri) = onSelected(uri)
            })
        }

        private fun open(context: Context, pickerType: PickerType, callback: Callback) {
            this.callback = callback
            this.pickerType = pickerType
            Intent(context, PickerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            when {
                result.resultCode == RESULT_OK && result.data?.data != null -> {
                    callback?.onSelected(result.data!!.data!!)
                }
                else -> callback?.onCancelled()
            }
        } finally {
            callback = null
            pickerType = null
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = when (pickerType) {
            PickerType.Folder -> Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            PickerType.File -> Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            null -> {
                callback?.onCancelled()
                finish()
                return
            }
        }
        pickerLauncher.launch(intent)
    }

    override fun onDestroy() {
        callback?.onCancelled()
        callback = null
        pickerType = null
        super.onDestroy()
    }
}