package dev.brahmkshatriya.echo

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationBarView
import dev.brahmkshatriya.echo.databinding.ActivityMainBinding
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.setupIntents
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.setupNavBarAndInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.setupPlayerBehavior
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.setupSnackBar
import dev.brahmkshatriya.echo.ui.exceptions.ExceptionUtils.setupExceptionHandler
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel.Companion.updateExtensions
import dev.brahmkshatriya.echo.utils.PermsUtils.checkAppPermissions
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val uiViewModel by viewModel<UiViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge(
            SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            if (isNightMode()) SystemBarStyle.dark(TRANSPARENT)
            else SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
        )

        setupNavBarAndInsets(uiViewModel, binding.root, binding.navView as NavigationBarView)
        setupPlayerBehavior(uiViewModel, binding.playerFragmentContainer)
        setupIntents(uiViewModel)
        setupExceptionHandler(setupSnackBar(uiViewModel, binding.root))
        checkAppPermissions()
        updateExtensions()
    }
}