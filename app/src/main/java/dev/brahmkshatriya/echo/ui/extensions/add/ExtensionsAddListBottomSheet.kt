package dev.brahmkshatriya.echo.ui.extensions.add

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.databinding.DialogExtensionsAddListBinding
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionsAddListBottomSheet : BottomSheetDialogFragment() {
    var binding by autoCleared<DialogExtensionsAddListBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogExtensionsAddListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var clicked = false
    private val viewModel by activityViewModel<AddViewModel>()
    private val extensionsViewModel by activityViewModel<ExtensionsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val extensionListAdapter = ExtensionsAddListAdapter { item, isChecked ->
            viewModel.toggleItem(item, isChecked)
        }

        val headerAdapter = ExtensionsAddListAdapter.Header(
            object : ExtensionsAddListAdapter.Header.Listener {
                override fun onClose() = dismiss()
                override fun onSelectAllChanged(select: Boolean) {
                    viewModel.selectAll(select)
                }
            }
        )

        val footerAdapter = ExtensionsAddListAdapter.Footer {
            clicked = true
            dismiss()
        }
        binding.root.adapter = ConcatAdapter(headerAdapter, extensionListAdapter, footerAdapter)
        observe(viewModel.addingFlow) {
            val list = viewModel.getList()
            extensionListAdapter.submitList(list)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.download(clicked, extensionsViewModel)
        super.onDismiss(dialog)
    }
}
