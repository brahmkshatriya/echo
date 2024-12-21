package dev.brahmkshatriya.echo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.ActivityExceptionBinding
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.getDetails
import dev.brahmkshatriya.echo.utils.restartApp
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class ExceptionActivity : AppCompatActivity() {

    val binding by lazy { ActivityExceptionBinding.inflate(layoutInflater) }
    val exception: String by lazy {
        intent.getStringExtra("stackTrace") ?: "Unknown Stack Trace"
    }
    val uiViewModel by viewModels<UiViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            uiViewModel.setSystemInsets(this, insets)
            binding.fabContainer.applyInsets(uiViewModel.systemInsets.value)
            insets
        }
        supportFragmentManager.commit {
            replace(
                R.id.exceptionFragmentContainer,
                ExceptionFragment.newInstance(
                    ExceptionDetails(
                        getString(R.string.app_crashed),
                        exception
                    )
                )
            )
        }
        binding.restartApp.setOnClickListener { restartApp() }
    }

    companion object {
        fun start(context: Context, exception: Throwable) {
            val intent = Intent(context, ExceptionActivity::class.java).apply {
                putExtra("stackTrace", context.getDetails(exception))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }

    @Serializable
    class ExceptionDetails(val title: String, val causedBy: String)
}