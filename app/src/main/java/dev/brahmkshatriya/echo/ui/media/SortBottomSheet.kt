package dev.brahmkshatriya.echo.ui.media

import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dev.brahmkshatriya.echo.databinding.DialogSortBinding
import dev.brahmkshatriya.echo.ui.media.adapter.SearchHeaderAdapter.Companion.loadedTracks
import dev.brahmkshatriya.echo.ui.media.adapter.SearchHeaderAdapter.Companion.trackSortState
import dev.brahmkshatriya.echo.ui.media.adapter.TrackSort
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.android.ext.android.getKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.viewmodel.resolveViewModel
import kotlin.reflect.KClass

class SortBottomSheet : BottomSheetDialogFragment() {
    private val args by lazy { requireArguments() }

    @Suppress("UNCHECKED_CAST")
    @OptIn(KoinInternalApi::class)
    private val vm by lazy {
        val clazz = Class.forName(args.getString("vm")!!).kotlin
        resolveViewModel(
            clazz as KClass<out ViewModel>,
            requireParentFragment().viewModelStore,
            extras = defaultViewModelCreationExtras,
            qualifier = null,
            parameters = null,
            scope = getKoinScope()
        )
    }

    private val loadedTracks by lazy { vm.loadedTracks }
    private val sortState by lazy { vm.trackSortState }

    private var binding by autoCleared<DialogSortBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var selectedTrackSort: TrackSort? = null
    private fun applySorts() {
        val list = loadedTracks.value ?: listOf()
        val state = sortState.value
        selectedTrackSort = state?.trackSort
        val available = TrackSort.getSorts(list)
        val checked = available.indexOf(state?.trackSort)
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
                selectedTrackSort = available.getOrNull(checkedIds.firstOrNull() ?: -1)
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
            binding.saveCheckbox.isChecked = it?.save ?: false
            binding.reversedSwitch.isChecked = it?.reversed ?: false
        }
        binding.saveContainer.setOnClickListener {
            binding.saveCheckbox.isChecked = !binding.saveCheckbox.isChecked
        }
        binding.reversedContainer.setOnClickListener {
            binding.reversedSwitch.isChecked = !binding.reversedSwitch.isChecked
        }
        binding.apply.setOnClickListener {
            sortState.value = TrackSort.State(
                trackSort = selectedTrackSort,
                reversed = binding.reversedSwitch.isChecked,
                save = binding.saveCheckbox.isChecked
            )
            dismiss()
        }
        binding.topAppBar.setNavigationOnClickListener {
            dismiss()
        }
        binding.topAppBar.setOnCreateContextMenuListener { _, _, _ ->
            sortState.value = TrackSort.State()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult("sort", Bundle().apply {
            putBoolean("changed", true)
        })
    }

    companion object {
        fun newInstance(kClass: KClass<out ViewModel>): SortBottomSheet {
            return SortBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("vm", kClass.qualifiedName!!)
                }
            }
        }
    }
}