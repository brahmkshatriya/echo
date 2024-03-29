package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.ActivityMainBinding
import dev.brahmkshatriya.echo.player.PlaybackService
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.ui.applyInsetsToPlayerUI
import dev.brahmkshatriya.echo.player.ui.connectPlayerToUI
import dev.brahmkshatriya.echo.player.ui.createPlayerUI
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.snackbar.initSnackBar
import dev.brahmkshatriya.echo.utils.checkPermissions
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.tryWith
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    @Inject lateinit var preferences: SharedPreferences
    val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var controllerFuture: ListenableFuture<MediaBrowser>? = null
    private val playerViewModel: PlayerViewModel by viewModels()
    private val extensionViewModel: ExtensionViewModel by viewModels()

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge(
            SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            if (isNightMode()) SystemBarStyle.dark(TRANSPARENT)
            else SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
        )

        checkPermissions(this)

        val navView = binding.navView as NavigationBarView
        val navHostFragment = binding.navHostFragment.getFragment<NavHostFragment>()
        navView.setupWithNavController(navHostFragment.navController)

        initSnackBar(this)

        extensionViewModel.initialize()
        createPlayerUI(this)
        applyInsetsToPlayerUI(this)

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        MediaBrowser.Builder(this, sessionToken).buildAsync().also {
            controllerFuture = it
            val listener = Runnable {
                val browser = tryWith(false) { it.get() } ?: return@Runnable
                connectPlayerToUI(this, browser)
            }
            it.addListener(listener, ContextCompat.getMainExecutor(this))
        }
    }

    fun isNightMode() =
        resources.configuration.uiMode and UI_MODE_NIGHT_MASK != UI_MODE_NIGHT_NO

    override fun onNewIntent(intent: Intent?) {
        intent?.hasExtra("fromNotification")?.let {
            emit(playerViewModel.fromNotification) { it }
            intent.removeExtra("fromNotification")
        }
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaBrowser.releaseFuture(it) }
    }
}