package dev.brahmkshatriya.echo.ui.snackbar

import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.viewModels
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.newui.exception.openException
import dev.brahmkshatriya.echo.utils.observe

fun initSnackBar(activity: MainActivity) {
    val snackBarViewModel: SnackBarViewModel by activity.viewModels()

    activity.apply {
        val navController = binding.navHostFragment.getFragment<NavHostFragment>().navController

    }
}

