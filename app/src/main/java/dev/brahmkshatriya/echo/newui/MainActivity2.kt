package dev.brahmkshatriya.echo.newui

import android.content.ComponentName
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.ActivityMain2Binding
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import dev.brahmkshatriya.echo.viewmodels.InsetsViewModel
import dev.brahmkshatriya.echo.player.PlaybackService
import dev.brahmkshatriya.echo.player.ui.connectPlayerToUI
import dev.brahmkshatriya.echo.utils.checkPermissions
import dev.brahmkshatriya.echo.utils.isNightMode
import dev.brahmkshatriya.echo.utils.tryWith

@AndroidEntryPoint
class MainActivity2 : AppCompatActivity() {

    val binding by lazy {
        ActivityMain2Binding.inflate(layoutInflater)
    }

    private val extensionViewModel by viewModels<ExtensionViewModel>()

    private var controllerFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val sessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val future = MediaBrowser.Builder(application, sessionToken).buildAsync()
        future.addListener({
            val browser = tryWith(false) { future.get() } ?: return@addListener
            connectPlayerToUI(this, browser)
        }, ContextCompat.getMainExecutor(this))
        controllerFuture = future

        extensionViewModel.initialize()

        enableEdgeToEdge(
            SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            if (isNightMode()) SystemBarStyle.dark(TRANSPARENT)
            else SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
        )

        checkPermissions(this)

        val navView = binding.navView as NavigationBarView
        val navHostFragment = binding.navHostFragment.getFragment<NavHostFragment>()
        navView.setupWithNavController(navHostFragment.navController)

        val insetsViewModel by viewModels<InsetsViewModel>()
        val rail = binding.navView is NavigationRailView

        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            val isNavFragment = destination.id == navView.selectedItemId
            binding.navView.isVisible = isNavFragment
            binding.navViewOutline?.isVisible = isNavFragment
            insetsViewModel.setNavInsets(this, isNavFragment, rail)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            insetsViewModel.setSystemInsets(this, insets)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaBrowser.releaseFuture(it) }
    }
}