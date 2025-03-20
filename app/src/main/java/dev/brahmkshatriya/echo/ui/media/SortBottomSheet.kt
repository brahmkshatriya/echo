package dev.brahmkshatriya.echo.ui.media

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.DialogSortBinding
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.flow.MutableStateFlow

class SortBottomSheet(
    val sortState: MutableStateFlow<Sort.State>,
    val loadedTracks: MutableStateFlow<List<Track>?>,
    val apply: () -> Unit
) : BottomSheetDialogFragment() {

    private var binding by autoCleared<DialogSortBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var selectedSort: Sort? = null
    private fun applySorts() {
        val list = loadedTracks.value ?: listOf()
        val state = sortState.value
        selectedSort = state.sort
        val available = Sort.getSorts(list)
        val checked = available.indexOf(state.sort)
        binding.sortChipGroup.run {
            removeAllViews()
            available.forEachIndexed { index, t ->
                val chip = Chip(context)
                chip.id = index
                chip.text = getString(t.title)
                chip.ellipsize = TextUtils.TruncateAt.MIDDLE
                chip.isCheckable = true
                addView(chip)
                if (index == checked) check(chip.id)
            }
            setOnCheckedStateChangeListener { _, checkedIds ->
                selectedSort = available.getOrNull(checkedIds.firstOrNull() ?: -1)
            }
        }
        binding.filter.isVisible = false
        binding.filterGroup.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observe(loadedTracks) {
            binding.progressIndicator.isVisible = it == null
            applySorts()
        }
        observe(sortState) {
            applySorts()
            binding.saveCheckbox.isChecked = it.save
            binding.reversedSwitch.isChecked = it.reversed
        }
        binding.saveContainer.setOnClickListener {
            binding.saveCheckbox.isChecked = !binding.saveCheckbox.isChecked
        }
        binding.reversedContainer.setOnClickListener {
            binding.reversedSwitch.isChecked = !binding.reversedSwitch.isChecked
        }
        binding.apply.setOnClickListener {
            sortState.value = Sort.State(
                sort = selectedSort,
                reversed = binding.reversedSwitch.isChecked,
                save = binding.saveCheckbox.isChecked
            )
            apply()
            dismiss()
        }
    }
}