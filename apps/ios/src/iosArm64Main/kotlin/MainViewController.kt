package dev.brahmkshatriya.echo

import androidx.compose.ui.window.ComposeUIViewController
import dev.brahmkshatriya.echo.app.ui.App
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
