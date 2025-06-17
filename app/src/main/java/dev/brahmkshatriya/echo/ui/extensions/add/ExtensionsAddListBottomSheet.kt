package dev.brahmkshatriya.echo.ui.extensions.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogExtensionAddBinding
import dev.brahmkshatriya.echo.databinding.DialogExtensionsAddListBinding
import dev.brahmkshatriya.echo.extensions.Updater
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ExtensionsAddListBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(list: List<Updater.ExtensionAssetResponse>) =
            ExtensionsAddListBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerialized("list", list)
                }
            }
    }

    var binding by autoCleared<DialogExtensionsAddListBinding>()
    val args by lazy { requireArguments() }
    val list by lazy { args.getSerialized<List<Updater.ExtensionAssetResponse>>("list")!! }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogExtensionsAddListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val viewModel by viewModel<AddViewModel> { parametersOf(list) }
    private val extensionViewModel by activityViewModel<ExtensionsViewModel>()

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
            val selected = viewModel.listFlow.value.filter { it.isChecked }.map { it.item }
            extensionViewModel.addExtensions(selected)
            dismiss()
        }
        binding.root.adapter =
            ConcatAdapter(headerAdapter, extensionListAdapter, footerAdapter)
        observe(viewModel.listFlow) {
            extensionListAdapter.submitList(it)
        }
    }

    class LinkFile : BottomSheetDialogFragment() {

        var binding by autoCleared<DialogExtensionAddBinding>()
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = DialogExtensionAddBinding.inflate(inflater)
            return binding.root
        }

        val viewModel by activityViewModel<ExtensionsViewModel>()
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val context = requireActivity()
            binding.topAppBar.setNavigationOnClickListener { dismiss() }
            binding.installationTypeGroup.addOnButtonCheckedListener { group, _, _ ->
                val isLink = group.checkedButtonId == R.id.linkAdd
                binding.textInputLayout.isVisible = isLink
            }

            binding.editText.setOnEditorActionListener { _, _, _ ->
                binding.installButton.performClick()
                true
            }

            binding.installButton.setOnClickListener {
                val isLink = binding.installationTypeGroup.checkedButtonId == R.id.linkAdd
                if (!isLink) {
                    viewModel.addFromFile(context)
                    dismiss()
                } else {
                    val link = binding.editText.text.toString()
                    if (link.isEmpty()) return@setOnClickListener
                    viewModel.addFromLinkOrCode(link)
                }
            }

            observe(viewModel.addingFlow) {
                when (it) {
                    Updater.AddState.Init -> {
                        binding.loading.root.isVisible = false
                        binding.nestedScrollView.isVisible = true
                    }

                    Updater.AddState.Loading -> {
                        binding.loading.root.isVisible = true
                        binding.nestedScrollView.isVisible = false
                    }

                    is Updater.AddState.AddList -> {
                        if (it.list != null) {
                            if (it.list.isEmpty()) createSnack(R.string.list_is_empty)
                            else newInstance(it.list).show(context.supportFragmentManager, null)
                        }
                        viewModel.addingFlow.value = Updater.AddState.Init
                        dismiss()
                    }
                }
            }
        }
    }
}
