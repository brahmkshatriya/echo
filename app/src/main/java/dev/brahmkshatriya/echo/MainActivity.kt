package dev.brahmkshatriya.echo

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.databinding.ActivityMainBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.utils.animateTranslation
import dev.brahmkshatriya.echo.utils.checkPermissions
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.createNavDrawable
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.listenFuture
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel.Companion.connectBrowserToUI
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.configureSnackBar
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isNightMode
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.setupPlayerBehavior
import kotlin.math.max
import kotlin.math.min


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val uiViewModel by viewModels<UiViewModel>()
    private val playerViewModel by viewModels<PlayerViewModel>()

    private var controllerFuture: ListenableFuture<MediaBrowser>? = null

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

        navView.setOnItemSelectedListener {
            uiViewModel.navigation.value = uiViewModel.navIds.indexOf(it.itemId)
            true
        }
        navView.setOnItemReselectedListener {
            emit(uiViewModel.navigationReselected) { uiViewModel.navIds.indexOf(it.itemId) }
        }
        val isRail = binding.navView is NavigationRailView

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            uiViewModel.setSystemInsets(this, insets)
            navView.createNavDrawable(uiViewModel.systemInsets.value.bottom)
            insets
        }

        supportFragmentManager.addOnBackStackChangedListener {
            uiViewModel.isMainFragment.value = supportFragmentManager.backStackEntryCount == 0
        }

        binding.navView.post {
            collect(uiViewModel.isMainFragment) { isMainFragment ->
                val insets =
                    uiViewModel.setPlayerNavViewInsets(this, isMainFragment, isRail)
                val visible = uiViewModel.playerSheetState.value.let {
                    it == STATE_COLLAPSED || it == STATE_HIDDEN
                }
                navView.animateTranslation(isRail, isMainFragment, visible) {
                    uiViewModel.setNavInsets(insets)
                }
            }
        }

        observe(uiViewModel.playerSheetState) {
            uiViewModel.setPlayerInsets(this, it != STATE_HIDDEN)
        }

        val collapsedPlayerHeight = resources.getDimension(R.dimen.bottom_player_peek_height)
        observe(uiViewModel.playerSheetOffset) {
            if (it != 0f && isRail)
                navView.translationY = -(1 + min(it, 0f)) * collapsedPlayerHeight
            if (!uiViewModel.isMainFragment.value) return@observe
            val offset = max(0f, it)
            if (isRail) navView.translationX = -navView.width * offset
            else navView.translationY = navView.height * offset
        }

        setupPlayerBehavior(uiViewModel, binding.playerFragmentContainer)
        configureSnackBar(binding.navView)

        val sessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val browserFuture = MediaBrowser.Builder(application, sessionToken).buildAsync()
        listenFuture(browserFuture) {
            val browser = it.getOrElse { e ->
                e.printStackTrace()
                return@listenFuture
            }
            connectBrowserToUI(browser, playerViewModel)
        }

        controllerFuture = browserFuture

        intent?.onIntent()
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaBrowser.releaseFuture(it) }
    }

    private fun Intent.onIntent() {
        val fromNotif = hasExtra("fromNotification")
        if (fromNotif) {
            uiViewModel.fromNotification.value = true
            return
        }
        println("Intent: $data")
        val uri = data
        if (uri != null) {
            fun createSnack(id: Int) {
                val snackbar by viewModels<SnackBar>()
                val message = getString(id)
                println("bruh : $message")
                snackbar.create(SnackBar.Message(message))
            }

            val extensionType = uri.host
            println(type)
            when (extensionType) {
                "music" -> {
                    val extensionId = uri.pathSegments.firstOrNull()
                    if (extensionId == null) {
                        createSnack(R.string.error_no_client)
                        return
                    }
                    val type = uri.pathSegments.getOrNull(1)
                    val id = uri.pathSegments.getOrNull(2)
                    if (id == null) {
                        createSnack(R.string.error_no_id)
                        return
                    }
                    val name = uri.getQueryParameter("name").orEmpty()
                    val item: EchoMediaItem? = when (type) {
                        "user" -> User(id, name).toMediaItem()
                        "artist" -> Artist(id, name).toMediaItem()
                        "track" -> Track(id, name).toMediaItem()
                        "album" -> Album(id, name).toMediaItem()
                        "playlist" -> Playlist(id, name, false).toMediaItem()
                        else -> null
                    }
                    if (item == null) {
                        createSnack(R.string.error_invalid_type)
                        return
                    }
                    openFragment(ItemFragment.newInstance(extensionId, item))
                }

                else -> {
                    createSnack(R.string.invalid_extension_host)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.onIntent()
        super.onNewIntent(intent)
    }

}