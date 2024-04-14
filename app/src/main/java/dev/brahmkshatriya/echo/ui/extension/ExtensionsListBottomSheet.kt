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
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogExtensionsListBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

class ExtensionsListBottomSheet : BottomSheetDialogFragment() {

    private var binding by autoCleared<DialogExtensionsListBinding>()
    private val viewModel by activityViewModels<ExtensionViewModel>()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionsListBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.topAppBar.setNavigationOnClickListener { dismiss() }

        binding.addExtension.isEnabled = false

        val listener = object : OnButtonCheckedListener {
            var map: Map<Int, ExtensionClient> = mapOf()
            var enabled = false
            override fun onButtonChecked(
                group: MaterialButtonToggleGroup?,
                checkedId: Int,
                isChecked: Boolean
            ) {
                if (isChecked && enabled) map[checkedId]?.let {
                    viewModel.setExtension(it)
                    dismiss()
                }
            }
        }
        binding.buttonToggleGroup.addOnButtonCheckedListener(listener)
        val extensionFlow = viewModel.extensionListFlow.flow
        collect(extensionFlow) { clientList ->
            binding.buttonToggleGroup.removeAllViews()
            binding.progressIndicator.isVisible = clientList == null
            listener.enabled = false
            val list = clientList ?: emptyList()

            val map = list.mapIndexed { index, extension ->
                val button = ButtonExtensionBinding.inflate(
                    layoutInflater,
                    binding.buttonToggleGroup,
                    false
                ).root
                val metadata = extension.metadata
                button.text = metadata.name
                binding.buttonToggleGroup.addView(button)
                button.isChecked = extension == viewModel.currentExtension
                metadata.iconUrl.loadWith(button, R.drawable.ic_extension) { button.icon = it }
                button.id = index
                index to extension
            }.toMap()

            val checked = map.filter { it.value == viewModel.currentExtension }.keys
                .firstOrNull()

            listener.map = map
            if (checked != null) binding.buttonToggleGroup.check(checked)
            listener.enabled = true

        }
    }

}