package dev.brahmkshatriya.echo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {
    var collapsePlayer : (()->Unit)? = null
    val playerCollapsed = MutableStateFlow(true)
}