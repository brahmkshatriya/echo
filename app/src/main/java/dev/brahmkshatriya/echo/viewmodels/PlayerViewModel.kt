package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kotlinx.coroutines.flow.MutableStateFlow

class PlayerViewModel : ViewModel() {
    val playerSheetState = MutableStateFlow(STATE_HIDDEN)
    val infoSheetState = MutableStateFlow(STATE_COLLAPSED)

    val playerSheetSlide = MutableStateFlow(0f)
}