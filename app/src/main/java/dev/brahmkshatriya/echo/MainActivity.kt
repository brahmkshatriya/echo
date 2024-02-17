package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.ActivityMainBinding
import dev.brahmkshatriya.echo.player.PlaybackService
import dev.brahmkshatriya.echo.player.initPlayer
import dev.brahmkshatriya.echo.ui.utils.checkPermissions
import dev.brahmkshatriya.echo.ui.utils.emit
import dev.brahmkshatriya.echo.ui.utils.updateBottomMarginWithSystemInsets
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    var fromNotification: MutableSharedFlow<Boolean> = MutableSharedFlow()

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets -> insets }

        checkPermissions(this)

        val navView = binding.navView as NavigationBarView
        val navHostFragment = binding.navHostFragment.getFragment<NavHostFragment>()
        navView.setupWithNavController(navHostFragment.navController)
        updateBottomMarginWithSystemInsets(binding.navHostFragment)

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaBrowser.Builder(this, sessionToken).buildAsync()
        val listener = Runnable { initPlayer(this, controllerFuture.get()) }
        controllerFuture.addListener(listener, ContextCompat.getMainExecutor(this))
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.hasExtra("fromNotification")?.let {
            emit(fromNotification) { it }
        }
        super.onNewIntent(intent)
    }
}