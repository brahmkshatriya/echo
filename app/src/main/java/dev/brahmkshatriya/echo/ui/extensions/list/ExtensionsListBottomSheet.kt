package dev.brahmkshatriya.echo.ui.extensions.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogExtensionsListBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoFragment.Companion.getType
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.add.ExtensionsAddListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.manage.ManageExtensionsFragment
import dev.brahmkshatriya.echo.ui.player.lyrics.LyricsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionsListBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(type: ExtensionType) = ExtensionsListBottomSheet().apply {
            arguments = Bundle().apply {
                putString("type", type.name)
            }
        }
    }

    private var binding by autoCleared<DialogExtensionsListBinding>()
    private val args by lazy { requireArguments() }
    private val type by lazy { ExtensionType.valueOf(args.getString("type")!!) }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionsListBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.topAppBar.title = getString(R.string.x_extensions, getString(getType(type)))

        binding.addExtension.setOnClickListener {
            dismiss()
            ExtensionsAddListBottomSheet.LinkFile().show(parentFragmentManager, null)
        }

        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_manage_ext -> {
                    dismiss()
                    requireActivity().openFragment(ManageExtensionsFragment())
                    true
                }

                else -> false
            }
        }

        val viewModel = when (type) {
            ExtensionType.MUSIC -> activityViewModel<ExtensionsViewModel>().value
            ExtensionType.LYRICS -> activityViewModel<LyricsViewModel>().value
            else -> throw IllegalStateException("Not supported")
        }

        val listener = object : OnButtonCheckedListener {
            var enabled = false
            override fun onButtonChecked(
                group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean
            ) {
                if (isChecked && enabled) {
                    viewModel.onExtensionSelected(checkedId)
                    dismiss()
                }
            }
        }
        binding.buttonToggleGroup.addOnButtonCheckedListener(listener)
        val extensionFlow = viewModel.extensionsFlow
        observe(extensionFlow) { list ->
            binding.buttonToggleGroup.removeAllViews()
            binding.progressIndicator.isVisible = list == null
            listener.enabled = false
            val extensions = list ?: emptyList()

            extensions.forEachIndexed { index, extension ->
                if (!extension.isEnabled) return@forEachIndexed
                val button = ButtonExtensionBinding.inflate(
                    layoutInflater,
                    binding.buttonToggleGroup,
                    false
                ).root
                button.text = extension.name
                binding.buttonToggleGroup.addView(button)
                button.isChecked = extension == viewModel.currentSelectionFlow.value
                extension.metadata.iconUrl?.toImageHolder().loadAsCircle(button) {
                    if (it != null) {
                        button.icon = it
                        button.iconTint = null
                    } else button.setIconResource(R.drawable.ic_extension)
                }
                button.id = index
            }

            val checked = extensions.indexOf(viewModel.currentSelectionFlow.value)
                .takeIf { it != -1 }
            if (checked != null) binding.buttonToggleGroup.check(checked)
            listener.enabled = true
        }
    }

}