package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.databinding.ActivityMainBinding
import dev.brahmkshatriya.echo.player.PlaybackService
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.createPlayer
import dev.brahmkshatriya.echo.player.startPlayer
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.utils.checkPermissions
import dev.brahmkshatriya.echo.ui.utils.emit
import dev.brahmkshatriya.echo.ui.utils.tryWith
import dev.brahmkshatriya.echo.ui.utils.updateBottomMarginWithSystemInsets
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    @Inject
    lateinit var pluginRepo: PluginRepo<ExtensionClient>

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

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets -> insets }

        checkPermissions(this)

        val navView = binding.navView as NavigationBarView
        val navHostFragment = binding.navHostFragment.getFragment<NavHostFragment>()
        navView.setupWithNavController(navHostFragment.navController)
        updateBottomMarginWithSystemInsets(binding.navHostFragment)

        if (extensionViewModel.extensionListFlow == null) {
            extensionViewModel.extensionListFlow = pluginRepo.getAllPlugins { e ->
                e.message?.let {
                    val snack = Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT)
                    if (binding.navView is BottomNavigationView)
                        snack.setAnchorView(binding.navView)
                    snack.show()
                }
            }
        }
        createPlayer(this)
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.hasExtra("fromNotification")?.let {
            emit(playerViewModel.fromNotification) { it }
        }
        super.onNewIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        MediaBrowser.Builder(this, sessionToken).buildAsync().also {
            controllerFuture = it
            val listener = Runnable {
                tryWith {
                    startPlayer(this, it.get())
                }
            }
            it.addListener(listener, ContextCompat.getMainExecutor(this))
        }
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaBrowser.releaseFuture(it) }
    }
}