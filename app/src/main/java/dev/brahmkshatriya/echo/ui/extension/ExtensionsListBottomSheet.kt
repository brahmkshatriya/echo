package dev.brahmkshatriya.echo.ui.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogExtensionsBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.player.lyrics.LyricsViewModel
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.image.loadAsCircle
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

class ExtensionsListBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(type: ExtensionType) = ExtensionsListBottomSheet().apply {
            arguments = Bundle().apply {
                putString("type", type.name)
            }
        }
    }

    private var binding by autoCleared<DialogExtensionsBinding>()
    private val args by lazy { requireArguments() }
    private val type by lazy { ExtensionType.valueOf(args.getString("type")!!) }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionsBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.addExtension.setOnClickListener {
            dismiss()
            ExtensionsAddListBottomSheet.LinkFile().show(parentFragmentManager, null)
        }

        binding.topAppBar.setOnMenuItemClickListener {
            when(it.itemId){
               R.id.menu_manage_ext -> {
                   dismiss()
                   requireActivity().openFragment(ManageExtensionsFragment())
                   true
               }
                else -> false
            }
        }

        val viewModel = when (type) {
            ExtensionType.LYRICS -> activityViewModels<LyricsViewModel>().value
            ExtensionType.MUSIC -> activityViewModels<ExtensionViewModel>().value
            else -> throw IllegalStateException("Not supported")
        }

        val listener = object : OnButtonCheckedListener {
            var map: Map<Int, Metadata> = mapOf()
            var enabled = false
            override fun onButtonChecked(
                group: MaterialButtonToggleGroup?,
                checkedId: Int,
                isChecked: Boolean
            ) {
                if (isChecked && enabled) map[checkedId]?.let {
                    viewModel.onClientSelected(it.id)
                    dismiss()
                }
            }
        }
        binding.buttonToggleGroup.addOnButtonCheckedListener(listener)
        val extensionFlow = viewModel.metadataFlow
        collect(extensionFlow) { clientList ->
            binding.buttonToggleGroup.removeAllViews()
            binding.progressIndicator.isVisible = clientList == null
            listener.enabled = false
            val list = clientList?.filter { it.enabled } ?: emptyList()

            val map = list.mapIndexed { index, metadata ->
                val button = ButtonExtensionBinding.inflate(
                    layoutInflater,
                    binding.buttonToggleGroup,
                    false
                ).root
                button.text = metadata.name
                binding.buttonToggleGroup.addView(button)
                button.isChecked = metadata.id == viewModel.currentFlow.value
                metadata.iconUrl?.toImageHolder().loadAsCircle(button) {
                    if (it != null) {
                        button.icon = it
                        button.iconTint = null
                    } else button.setIconResource(R.drawable.ic_extension)
                }
                button.id = index
                index to metadata
            }.toMap()

            val checked = map.filter { it.value.id == viewModel.currentFlow.value }.keys
                .firstOrNull()

            listener.map = map
            if (checked != null) binding.buttonToggleGroup.check(checked)
            listener.enabled = true

        }
    }

}